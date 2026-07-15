package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.exception.UploadException;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.service.BusinessProfileService;
import com.mybill.MyBill_Backend.service.CloudinaryUploadSignatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Slf4j
public class BusinessProfileController {

    private final BusinessProfileService service;
    private final CloudinaryUploadSignatureService cloudinaryUploadSignatureService;

    @GetMapping
    public ResponseEntity<BusinessProfile> getProfile() {
        BusinessProfile profile = service.getProfile();
        if (profile == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<BusinessProfile> saveOrUpdateProfile(@Valid @RequestBody BusinessProfile profile) {
        return ResponseEntity.ok(service.saveOrUpdateProfile(profile));
    }

    @PostMapping("/upload/{type}/signature")
    public ResponseEntity<Map<String, Object>> createCloudinaryUploadSignature(@PathVariable String type) {
        return ResponseEntity.ok(cloudinaryUploadSignatureService.createSignature(type).toResponse());
    }

    @PostMapping("/upload/{type}/metadata")
    public ResponseEntity<Map<String, String>> saveCloudinaryImageMetadata(
            @PathVariable String type,
            @Valid @RequestBody CloudinaryImageMetadataRequest request
    ) {
        BusinessProfileService.ImageField field = switch (type) {
            case "logo" -> BusinessProfileService.ImageField.LOGO;
            case "qr" -> BusinessProfileService.ImageField.QR;
            case "signature" -> BusinessProfileService.ImageField.SIGNATURE;
            default -> throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported image type");
        };

        BusinessProfile profile = service.updateCloudinaryImage(
                new BusinessProfileService.ImageMetadata(
                        request.secureUrl(),
                        request.publicId(),
                        request.resourceType(),
                        request.width(),
                        request.height(),
                        request.format()
                ),
                field
        );

        String path = switch (field) {
            case LOGO -> profile.getLogoPath();
            case QR -> profile.getQrImagePath();
            case SIGNATURE -> profile.getSignaturePath();
        };
        return ResponseEntity.ok(Map.of("path", path));
    }

    public record CloudinaryImageMetadataRequest(
            String secureUrl,
            String publicId,
            String resourceType,
            Integer width,
            Integer height,
            String format
    ) {
    }
}
