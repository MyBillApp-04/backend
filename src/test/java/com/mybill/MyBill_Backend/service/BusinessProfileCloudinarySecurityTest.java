package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BusinessProfileCloudinarySecurityTest {
    @Test
    void rejectsCrossTenantCloudinaryMetadataBeforePersistence() {
        BusinessProfileRepository repository = mock(BusinessProfileRepository.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        BusinessProfileService service = new BusinessProfileService(repository, securityUtils);
        ReflectionTestUtils.setField(service, "cloudinaryCloudName", "my-cloud");
        ReflectionTestUtils.setField(service, "cloudinaryApiSecret", "secret");

        var metadata = new BusinessProfileService.ImageMetadata(
                "https://res.cloudinary.com/my-cloud/image/upload/v1/mybill/99/logo_x.png",
                "mybill/99/logo_x", "image", 100, 100, "png", 100L, 1L, "0123456789012345678901234567890123456789");

        assertThatThrownBy(() -> service.updateCloudinaryImage(metadata, BusinessProfileService.ImageField.LOGO))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(repository);
    }
}
