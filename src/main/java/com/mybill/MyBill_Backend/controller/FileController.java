package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provides authenticated access to uploaded business files (logo, QR code, signature).
 *
 * <p>This controller replaces the former public {@code /uploads/**} static resource handler.
 * Every request requires a valid JWT token and ownership of the referenced file.
 *
 * <p>Endpoint: {@code GET /api/files/{filename}}
 *
 * <p>Response codes:
 * <ul>
 *   <li>{@code 200} – file streamed successfully</li>
 *   <li>{@code 401} – no valid JWT (handled by {@link com.mybill.MyBill_Backend.security.JwtAuthenticationFilter})</li>
 *   <li>{@code 403} – authenticated user does not own this file</li>
 *   <li>{@code 404} – file does not exist on disk</li>
 *   <li>{@code 500} – I/O error reading the file</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    /**
     * Serves an uploaded business file to the authenticated owner.
     *
     * @param filename the bare filename (e.g., {@code logo_abc123.jpg}),
     *                 extracted from the path variable
     * @return the file bytes with the appropriate {@code Content-Type} header
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        String safeFilename = fileService.requireSafeUploadFilename(filename);
        Resource resource = fileService.loadFileAsResource(safeFilename);
        String contentType = fileService.detectContentType(safeFilename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .inline()
                        .filename(safeFilename)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }
}
