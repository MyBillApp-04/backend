package com.mybill.MyBill_Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static resource configuration.
 *
 * <p>Note: The {@code /uploads/**} static resource handler was intentionally removed as part of
 * the V1.0 security hardening. File access is now secured through {@code /api/files/{filename}}
 * which requires JWT authentication and enforces ownership checks.
 *
 * <p>See {@code FileController} and {@code FileService} for the authenticated file serving
 * implementation.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    // No additional static resource handlers are registered.
    // Uploaded files are served exclusively through the authenticated FileController.
}