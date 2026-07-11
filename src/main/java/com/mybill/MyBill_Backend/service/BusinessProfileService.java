package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private final BusinessProfileRepository repository;
    private final SecurityUtils securityUtils;

    @Cacheable(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        return repository.findByUserId(userId).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile saveOrUpdateProfile(BusinessProfile profile) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();

        return repository.findByUserId(userId).map(existing -> {
            applyCoreFields(existing, profile, user);
            existing.setGstin(clean(profile.getGstin()));

            existing.setBankName(clean(profile.getBankName()));
            existing.setAccountNumber(clean(profile.getAccountNumber()));
            existing.setIfsc(clean(profile.getIfsc()));
            existing.setUpiId(clean(profile.getUpiId()));

            existing.setThankYouNote(clean(profile.getThankYouNote()));
            existing.setTermsAndConditions(clean(profile.getTermsAndConditions()));

            // Keep image paths safe: update only when frontend sends a non-empty path.
            String logoPath = clean(profile.getLogoPath());
            String qrPath = clean(profile.getQrImagePath());
            String signaturePath = clean(profile.getSignaturePath());
            if (logoPath != null) existing.setLogoPath(logoPath);
            if (qrPath != null) existing.setQrImagePath(qrPath);
            if (signaturePath != null) existing.setSignaturePath(signaturePath);

            return repository.saveAndFlush(existing);
        }).orElseGet(() -> {
            BusinessProfile newProfile = new BusinessProfile();
            newProfile.setUser(user);
            applyCoreFields(newProfile, profile, user);
            newProfile.setGstin(clean(profile.getGstin()));
            newProfile.setBankName(clean(profile.getBankName()));
            newProfile.setAccountNumber(clean(profile.getAccountNumber()));
            newProfile.setIfsc(clean(profile.getIfsc()));
            newProfile.setUpiId(clean(profile.getUpiId()));
            newProfile.setLogoPath(clean(profile.getLogoPath()));
            newProfile.setQrImagePath(clean(profile.getQrImagePath()));
            newProfile.setSignaturePath(clean(profile.getSignaturePath()));
            newProfile.setThankYouNote(clean(profile.getThankYouNote()));
            newProfile.setTermsAndConditions(clean(profile.getTermsAndConditions()));
            return repository.saveAndFlush(newProfile);
        });
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateLogoPath(String path) {
        return updateImagePath(path, ImageField.LOGO);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateQrImagePath(String path) {
        return updateImagePath(path, ImageField.QR);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateSignaturePath(String path) {
        return updateImagePath(path, ImageField.SIGNATURE);
    }

    private BusinessProfile updateImagePath(String path, ImageField field) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();
        String cleanPath = clean(path);

        BusinessProfile profile = repository.findByUserId(userId).orElseGet(() -> {
            BusinessProfile p = new BusinessProfile();
            p.setUser(user);
            applyCoreFields(p, new BusinessProfile(), user);
            return p;
        });

        switch (field) {
            case LOGO -> profile.setLogoPath(cleanPath);
            case QR -> profile.setQrImagePath(cleanPath);
            case SIGNATURE -> profile.setSignaturePath(cleanPath);
        }

        return repository.saveAndFlush(profile);
    }

    private void applyCoreFields(BusinessProfile target, BusinessProfile source, User user) {
        String userName = clean(user.getName());
        String userEmail = clean(user.getEmail());

        target.setBusinessName(required(source.getBusinessName(),
                userName != null ? userName : "My Business"));
        target.setOwnerName(required(source.getOwnerName(),
                userName != null ? userName : "Business Owner"));
        target.setAddress(required(source.getAddress(), "Not Provided"));
        target.setPhone(required(source.getPhone(), ""));
        target.setEmail(required(source.getEmail(),
                userEmail != null ? userEmail : "not-provided@mybill.local"));
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String required(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue != null ? cleanValue : fallback;
    }

    private enum ImageField {
        LOGO,
        QR,
        SIGNATURE
    }
}
