package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessProfileServiceTest {

    private final BusinessProfileRepository repository = mock(BusinessProfileRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final BusinessProfileService service = new BusinessProfileService(repository, securityUtils);

    @Test
    void saveOrUpdateProfileKeepsBlankPhoneValidationSafe() {
        User user = User.builder()
                .id(42L)
                .name("Owner")
                .email("owner@example.com")
                .build();
        BusinessProfile request = BusinessProfile.builder()
                .businessName("Acme")
                .ownerName("Owner")
                .address("Main Road")
                .phone("")
                .email("owner@example.com")
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(repository.findByUserId(42L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(BusinessProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessProfile saved = service.saveOrUpdateProfile(request);

        assertThat(saved.getPhone()).isEmpty();
    }

    @Test
    void testSaveOrUpdateProfileClearsImagesWhenNull() {
        User user = User.builder().id(42L).name("Owner").email("owner@example.com").build();
        BusinessProfile existing = BusinessProfile.builder()
                .id(1L)
                .user(user)
                .businessName("Acme")
                .ownerName("Owner")
                .address("Main Road")
                .logoPath("/uploads/logo.jpg")
                .qrImagePath("/uploads/qr.jpg")
                .signaturePath("/uploads/sig.jpg")
                .build();
                
        BusinessProfile request = BusinessProfile.builder()
                .businessName("Acme Updated")
                .ownerName("Owner Updated")
                .address("Main Road Updated")
                .logoPath(null)
                .qrImagePath(null)
                .signaturePath(null)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(repository.findByUserId(42L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(BusinessProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessProfile saved = service.saveOrUpdateProfile(request);

        assertThat(saved.getLogoPath()).isNull();
        assertThat(saved.getQrImagePath()).isNull();
        assertThat(saved.getSignaturePath()).isNull();
    }
}
