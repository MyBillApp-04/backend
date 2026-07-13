package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private static final String LOGO_FILENAME = "logo_123e4567-e89b-12d3-a456-426614174000.png";
    private static final String OTHER_LOGO_FILENAME = "logo_123e4567-e89b-12d3-a456-426614174001.png";
    private static final String QR_FILENAME = "qr_123e4567-e89b-12d3-a456-426614174002.jpg";
    private static final String SIGNATURE_FILENAME = "signature_123e4567-e89b-12d3-a456-426614174003.png";

    private BusinessProfileRepository businessProfileRepository;
    private SecurityUtils securityUtils;
    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        businessProfileRepository = mock(BusinessProfileRepository.class);
        securityUtils = mock(SecurityUtils.class);
        fileService = new FileService(businessProfileRepository, securityUtils);

        // Configure uploadDir to point to JUnit temp directory
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toAbsolutePath().toString());
    }

    @Test
    void loadFileAsResourceSucceedsWhenFileExistsAndIsOwned() throws IOException {
        Long userId = 42L;
        String filename = LOGO_FILENAME;
        Path file = tempDir.resolve(filename);
        Files.writeString(file, "test-content");

        BusinessProfile profile = BusinessProfile.builder()
                .logoPath("/uploads/" + filename)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Resource resource = fileService.loadFileAsResource(filename);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void loadFileAsResourceThrowsNotFoundWhenFileDoesNotExist() {
        Long userId = 42L;
        String filename = LOGO_FILENAME;

        BusinessProfile profile = BusinessProfile.builder()
                .logoPath("/uploads/" + filename)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> fileService.loadFileAsResource(filename))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void loadFileAsResourceThrowsForbiddenWhenFileIsNotOwned() throws IOException {
        Long userId = 42L;
        String filename = LOGO_FILENAME;
        Path file = tempDir.resolve(filename);
        Files.writeString(file, "test-content");

        BusinessProfile profile = BusinessProfile.builder()
                .logoPath("/uploads/" + OTHER_LOGO_FILENAME)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> fileService.loadFileAsResource(filename))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access to this file");
    }

    @Test
    void loadFileAsResourceThrowsNotFoundOnPathTraversalAttempt() {
        Long userId = 42L;
        String filename = "../etc/passwd";

        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        assertThatThrownBy(() -> fileService.loadFileAsResource(filename))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void detectsContentTypeCorrectly() {
        assertThat(fileService.detectContentType(QR_FILENAME)).isEqualTo("image/jpeg");
        assertThat(fileService.detectContentType(SIGNATURE_FILENAME)).isEqualTo("image/png");
    }
}
