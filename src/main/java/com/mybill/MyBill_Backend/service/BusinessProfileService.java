package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private static final Pattern UPLOAD_IMAGE_PATH = Pattern.compile(
            "^/uploads/(logo|qr|signature)_[0-9a-fA-F-]{36}\\.(png|jpg)$"
    );
    private static final Pattern CLOUDINARY_URL = Pattern.compile(
            "^https://res\\.cloudinary\\.com/[A-Za-z0-9_-]+/image/upload/.+"
    );

    private final BusinessProfileRepository repository;
    private final SecurityUtils securityUtils;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

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

            existing.setLogoPath(cleanExistingImagePath(profile.getLogoPath(), existing.getLogoPath()));
            existing.setQrImagePath(cleanExistingImagePath(profile.getQrImagePath(), existing.getQrImagePath()));
            existing.setSignaturePath(cleanExistingImagePath(profile.getSignaturePath(), existing.getSignaturePath()));

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
            newProfile.setLogoPath(cleanNewImagePath(profile.getLogoPath()));
            newProfile.setQrImagePath(cleanNewImagePath(profile.getQrImagePath()));
            newProfile.setSignaturePath(cleanNewImagePath(profile.getSignaturePath()));
            newProfile.setThankYouNote(clean(profile.getThankYouNote()));
            newProfile.setTermsAndConditions(clean(profile.getTermsAndConditions()));
            return repository.saveAndFlush(newProfile);
        });
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateLogoPath(String path) {
        return updateImagePath(ImageMetadata.legacy(path), ImageField.LOGO);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateQrImagePath(String path) {
        return updateImagePath(ImageMetadata.legacy(path), ImageField.QR);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateSignaturePath(String path) {
        return updateImagePath(ImageMetadata.legacy(path), ImageField.SIGNATURE);
    }

    @Transactional
    @CacheEvict(value = "businessProfiles", key = "@securityUtils.getCurrentUserId()")
    public BusinessProfile updateCloudinaryImage(ImageMetadata metadata, ImageField field) {
        return updateImagePath(metadata, field);
    }

    private BusinessProfile updateImagePath(ImageMetadata metadata, ImageField field) {
        Long userId = securityUtils.getCurrentUserId();
        User user = securityUtils.getCurrentUser();
        ImageMetadata cleanMetadata = cleanImageMetadata(metadata);

        BusinessProfile profile = repository.findByUserId(userId).orElseGet(() -> {
            BusinessProfile p = new BusinessProfile();
            p.setUser(user);
            applyCoreFields(p, new BusinessProfile(), user);
            return p;
        });

        switch (field) {
            case LOGO -> {
                profile.setLogoPath(cleanMetadata.secureUrl());
                profile.setLogoPublicId(cleanMetadata.publicId());
                profile.setLogoResourceType(cleanMetadata.resourceType());
                profile.setLogoWidth(cleanMetadata.width());
                profile.setLogoHeight(cleanMetadata.height());
                profile.setLogoFormat(cleanMetadata.format());
            }
            case QR -> {
                profile.setQrImagePath(cleanMetadata.secureUrl());
                profile.setQrImagePublicId(cleanMetadata.publicId());
                profile.setQrImageResourceType(cleanMetadata.resourceType());
                profile.setQrImageWidth(cleanMetadata.width());
                profile.setQrImageHeight(cleanMetadata.height());
                profile.setQrImageFormat(cleanMetadata.format());
            }
            case SIGNATURE -> {
                profile.setSignaturePath(cleanMetadata.secureUrl());
                profile.setSignaturePublicId(cleanMetadata.publicId());
                profile.setSignatureResourceType(cleanMetadata.resourceType());
                profile.setSignatureWidth(cleanMetadata.width());
                profile.setSignatureHeight(cleanMetadata.height());
                profile.setSignatureFormat(cleanMetadata.format());
            }
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

    private String cleanNewImagePath(String value) {
        String cleanValue = clean(value);
        if (cleanValue == null) return null;
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Image paths can only be changed through the authenticated upload endpoints"
        );
    }

    private String cleanExistingImagePath(String requestedValue, String currentValue) {
        String cleanRequested = clean(requestedValue);
        if (cleanRequested == null) return null;

        String cleanCurrent = clean(currentValue);
        if (cleanRequested.equals(cleanCurrent)) {
            return cleanRequested;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Image paths can only be changed through the authenticated upload endpoints"
        );
    }

    private String cleanUploadedImagePath(String value) {
        String cleanValue = clean(value);
        if (cleanValue == null) return null;
        if (!UPLOAD_IMAGE_PATH.matcher(cleanValue).matches() && !CLOUDINARY_URL.matcher(cleanValue).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid uploaded image path");
        }
        return cleanValue;
    }

    private ImageMetadata cleanImageMetadata(ImageMetadata metadata) {
        if (metadata == null) {
            return ImageMetadata.legacy(null);
        }
        String secureUrl = cleanUploadedImagePath(metadata.secureUrl());
        String publicId = clean(metadata.publicId());
        String resourceType = clean(metadata.resourceType());
        String format = clean(metadata.format());
        if (secureUrl != null && CLOUDINARY_URL.matcher(secureUrl).matches() && publicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cloudinary publicId is required");
        }
        if (secureUrl != null && CLOUDINARY_URL.matcher(secureUrl).matches()
                && cloudinaryCloudName != null && !cloudinaryCloudName.isBlank()
                && !secureUrl.startsWith("https://res.cloudinary.com/" + cloudinaryCloudName.trim() + "/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cloudinary image URL is not from the configured cloud");
        }
        if (resourceType != null && !"image".equals(resourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Cloudinary image resources are supported");
        }
        return new ImageMetadata(secureUrl, publicId, resourceType, metadata.width(), metadata.height(), format);
    }

    private String required(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue != null ? cleanValue : fallback;
    }

    public enum ImageField {
        LOGO,
        QR,
        SIGNATURE
    }

    public record ImageMetadata(
            String secureUrl,
            String publicId,
            String resourceType,
            Integer width,
            Integer height,
            String format
    ) {
        static ImageMetadata legacy(String path) {
            return new ImageMetadata(path, null, null, null, null, null);
        }
    }
}
