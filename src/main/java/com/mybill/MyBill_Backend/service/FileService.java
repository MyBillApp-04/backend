package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.exception.FileStorageException;
import com.mybill.MyBill_Backend.exception.ForbiddenException;
import com.mybill.MyBill_Backend.exception.NotFoundException;
import com.mybill.MyBill_Backend.repository.BusinessProfileRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides authenticated, ownership-checked access to uploaded business files
 * (logo, QR code, signature).
 *
 * <p>All file serving goes through this service. Files are never exposed as static
 * resources. The service verifies:
 * <ol>
 *   <li>The authenticated user owns a {@link BusinessProfile} that references the
 *       requested filename.</li>
 *   <li>The resolved path stays within the configured upload directory (path
 *       traversal prevention).</li>
 *   <li>The physical file exists on the filesystem.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Loads an uploaded file as a {@link Resource} after verifying the
     * requesting user is the owner of the business profile that references it.
     *
     * @param filename the bare filename (e.g., {@code logo_abc123.jpg})
     * @return a ready-to-stream {@link Resource}
     * @throws NotFoundException     if the file does not exist on the filesystem
     * @throws ForbiddenException    if the authenticated user does not own this file
     * @throws FileStorageException  if the file cannot be read due to an I/O error
     */
    public Resource loadFileAsResource(String filename) {
        Long userId = securityUtils.getCurrentUserId();

        // 1. Normalise and sanitise the filename — prevent path traversal
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(filename).normalize();

        if (!filePath.startsWith(uploadPath)) {
            // Treat path traversal the same as a missing file — do not confirm existence
            log.warn("Path traversal attempt detected for filename '{}' by user {}", filename, userId);
            throw new NotFoundException("File not found");
        }

        // 2. Verify ownership: the requesting user's business profile must reference this file
        String serverPath = "/uploads/" + filename;
        Optional<BusinessProfile> profile = businessProfileRepository.findByUserId(userId);

        boolean isOwned = profile.map(p ->
                Objects.equals(p.getLogoPath(), serverPath)
                        || Objects.equals(p.getQrImagePath(), serverPath)
                        || Objects.equals(p.getSignaturePath(), serverPath)
        ).orElse(false);

        if (!isOwned) {
            log.warn("User {} attempted to access file '{}' which does not belong to their profile",
                    userId, filename);
            throw new ForbiddenException("You do not have access to this file");
        }

        // 3. Check the file exists on disk
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("File '{}' not found on disk for user {}", filename, userId);
            throw new NotFoundException("File not found: " + filename);
        }

        // 4. Load as a URL resource
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.isReadable()) {
                throw new FileStorageException("File is not readable: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file '{}': {}", filename, e.getMessage());
            throw new FileStorageException("File could not be resolved: " + filename, e);
        }
    }

    /**
     * Determines the MIME type of a file by its extension.
     *
     * @param filename the filename
     * @return the MIME type string; defaults to {@code application/octet-stream}
     */
    public String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
