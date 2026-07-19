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

    private final SecurityUtils securityUtils;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

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
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp
                + apiSecret.trim());

        return new CloudinarySignature(cloudName, apiKey, timestamp, folder, publicId, signature);
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
            String folder,
            String publicId,
            String signature
    ) {
        public Map<String, Object> toResponse() {
            return Map.of(
                    "cloudName", cloudName,
                    "apiKey", apiKey,
                    "timestamp", timestamp,
                    "folder", folder,
                    "publicId", publicId,
                    "signature", signature
            );
        }
    }
}
