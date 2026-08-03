package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudinaryUploadSignatureServiceTest {
    @Test
    void signsOnlyBoundedImageUploadsInTheAuthenticatedUsersFolder() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenReturn(42L);
        CloudinaryUploadSignatureService service = new CloudinaryUploadSignatureService(securityUtils);
        ReflectionTestUtils.setField(service, "cloudName", "my-cloud");
        ReflectionTestUtils.setField(service, "apiKey", "key");
        ReflectionTestUtils.setField(service, "apiSecret", "secret");
        ReflectionTestUtils.setField(service, "uploadPreset", "restricted");

        var signature = service.createSignature("logo");

        assertThat(signature.folder()).isEqualTo("mybill/42/logo");
        assertThat(signature.publicId()).startsWith("logo_");
        assertThat(signature.allowedFormats()).isEqualTo("png,jpg,jpeg,webp");
        assertThat(signature.maxFileSize()).isEqualTo(5 * 1024 * 1024);
        assertThat(signature.expiresAt() - signature.timestamp()).isEqualTo(300);
    }
}
