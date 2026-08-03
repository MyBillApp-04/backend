package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryUploadSignatureService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("logo", "qr", "signature");
    private static final String ALLOWED_FORMATS = "png,jpg,jpeg,webp";
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    private final SecurityUtils securityUtils;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.cloudinary.upload-preset:mybill_restricted_images}")
    private String uploadPreset;

    public CloudinarySignature createSignature(String imageType) {
        if (!SUPPORTED_TYPES.contains(imageType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type");
        }
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary uploads are not configured");
        }

        long userId = securityUtils.getCurrentUserId();
        long timestamp = Instant.now().getEpochSecond();
        String folder = "mybill/" + userId + "/" + imageType;
        String publicId = imageType + "_" + UUID.randomUUID();
        String signature = sha1("folder=" + folder
                + "&allowed_formats=" + ALLOWED_FORMATS
                + "&max_file_size=" + MAX_BYTES
                + "&max_image_height=" + MAX_DIMENSION
                + "&max_image_width=" + MAX_DIMENSION
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp
                + "&upload_preset=" + uploadPreset
                + apiSecret.trim());

        return new CloudinarySignature(cloudName, apiKey, timestamp, timestamp + 300, folder, publicId, signature,
                uploadPreset, ALLOWED_FORMATS, MAX_BYTES, MAX_DIMENSION, MAX_DIMENSION);
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 digest is unavailable", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CloudinarySignature(
            String cloudName,
            String apiKey,
            long timestamp,
            long expiresAt,
            String folder,
            String publicId,
            String signature,
            String uploadPreset,
            String allowedFormats,
            long maxFileSize,
            int maxImageWidth,
            int maxImageHeight
    ) {
        public Map<String, Object> toResponse() {
            return Map.ofEntries(
                    Map.entry("cloudName", cloudName), Map.entry("apiKey", apiKey),
                    Map.entry("timestamp", timestamp), Map.entry("expiresAt", expiresAt),
                    Map.entry("folder", folder), Map.entry("publicId", publicId),
                    Map.entry("signature", signature), Map.entry("resourceType", "image"),
                    Map.entry("uploadPreset", uploadPreset), Map.entry("allowedFormats", allowedFormats),
                    Map.entry("maxFileSize", maxFileSize), Map.entry("maxImageWidth", maxImageWidth),
                    Map.entry("maxImageHeight", maxImageHeight)
            );
        }
    }
}
