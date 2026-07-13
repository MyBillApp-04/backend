package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.exception.FileStorageException;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileControllerTest {

    private FileService fileService;
    private FileController fileController;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        fileController = new FileController(fileService);
    }

    @Test
    void serveFileReturnsOkWithResourceAndHeaders() {
        String filename = "logo_123e4567-e89b-12d3-a456-426614174000.png";
        byte[] content = "fake image content".getBytes();
        Resource resource = new ByteArrayResource(content);

        when(fileService.requireSafeUploadFilename(eq(filename))).thenReturn(filename);
        when(fileService.loadFileAsResource(eq(filename))).thenReturn(resource);
        when(fileService.detectContentType(eq(filename))).thenReturn("image/png");

        ResponseEntity<Resource> response = fileController.serveFile(filename);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=\"" + filename + "\"");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    void serveFilePropagatesNotFoundException() {
        String filename = "logo_123e4567-e89b-12d3-a456-426614174000.png";
        when(fileService.requireSafeUploadFilename(eq(filename))).thenReturn(filename);
        when(fileService.loadFileAsResource(eq(filename)))
                .thenThrow(new NotFoundException("File not found: " + filename));

        assertThatThrownBy(() -> fileController.serveFile(filename))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void serveFilePropagatesForbiddenException() {
        String filename = "logo_123e4567-e89b-12d3-a456-426614174000.png";
        when(fileService.requireSafeUploadFilename(eq(filename))).thenReturn(filename);
        when(fileService.loadFileAsResource(eq(filename)))
                .thenThrow(new ForbiddenException("You do not have access to this file"));

        assertThatThrownBy(() -> fileController.serveFile(filename))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not have access to this file");
    }

    @Test
    void serveFilePropagatesFileStorageException() {
        String filename = "logo_123e4567-e89b-12d3-a456-426614174000.png";
        when(fileService.requireSafeUploadFilename(eq(filename))).thenReturn(filename);
        when(fileService.loadFileAsResource(eq(filename)))
                .thenThrow(new FileStorageException("File is not readable"));

        assertThatThrownBy(() -> fileController.serveFile(filename))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File is not readable");
    }
}
