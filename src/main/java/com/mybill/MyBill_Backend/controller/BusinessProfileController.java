package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.BusinessProfile;
import com.mybill.MyBill_Backend.exception.UploadException;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Slf4j
public class BusinessProfileController {

    private final BusinessProfileService service;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

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

    @PostMapping(value = "/upload/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String path = saveFile(file, "logo");
        service.updateLogoPath(path);
        return ResponseEntity.ok(Map.of("path", path));
    }

    @PostMapping(value = "/upload/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadQr(@RequestParam("file") MultipartFile file) {
        String path = saveFile(file, "qr");
        service.updateQrImagePath(path);
        return ResponseEntity.ok(Map.of("path", path));
    }

    @PostMapping(value = "/upload/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadSignature(@RequestParam("file") MultipartFile file) {
        String path = saveFile(file, "signature");
        service.updateSignaturePath(path);
        return ResponseEntity.ok(Map.of("path", path));
    }

    private String saveFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "IMAGE_EMPTY",
                    "Please select an image file.");
        }

        final long maxUploadBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxUploadBytes) {
            throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                    "Image must be 5 MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png").contains(contentType.toLowerCase())) {
            throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_TYPE_UNSUPPORTED",
                    "Please upload a JPG or PNG image.");
        }

        try {
            ImageIO.setUseCache(false);
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            NormalizedImage image = normalizeImage(file, prefix);

            String filename = prefix + "_" + UUID.randomUUID() + image.extension();
            Path target = dir.resolve(filename).normalize();

            if (!target.startsWith(dir)) {
                throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_PATH_INVALID",
                        "The image could not be saved. Please try again.");
            }

            writeNormalizedImageAtomically(image, target, prefix);
            return "/uploads/" + filename;
        } catch (UploadException | IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to process {} image upload: exception={} message={}",
                    prefix, e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_STORAGE_FAILED",
                    "The image could not be saved. Please try again.", e);
        }
    }

    private NormalizedImage normalizeImage(MultipartFile file, String kind) throws IOException {
        BufferedImage source;
        try (InputStream input = file.getInputStream()) {
            source = ImageIO.read(input);
        } catch (IOException e) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "IMAGE_READ_FAILED",
                    "The selected image could not be read. Please choose JPG or PNG.", e);
        }
        if (source == null) {
            throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_DECODE_FAILED",
                    "This image format isn't supported. Please use JPG or PNG.");
        }
        if ((long) source.getWidth() * source.getHeight() > 20_000_000L) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "IMAGE_DIMENSIONS_TOO_LARGE",
                    "Image dimensions are too large. Please choose a smaller image.");
        }

        int maxWidth = switch (kind) {
            case "logo" -> 600;
            case "signature" -> 520;
            case "qr" -> 256;
            default -> throw new IllegalArgumentException("Unsupported image type.");
        };
        int maxHeight = switch (kind) {
            case "logo" -> 240;
            case "signature" -> 180;
            case "qr" -> 256;
            default -> throw new IllegalArgumentException("Unsupported image type.");
        };

        BufferedImage scaled = scaleToFit(source, maxWidth, maxHeight, "signature".equals(kind));
        if ("qr".equals(kind)) {
            scaled = centerOnWhiteSquare(scaled, 256);
        }

        boolean preserveTransparency = "signature".equals(kind)
                || "qr".equals(kind)
                || scaled.getColorModel().hasAlpha();
        if (source != scaled) {
            source.flush();
        }

        return preserveTransparency
                ? new NormalizedImage(scaled, ".png", 1.0f)
                : new NormalizedImage(scaled, ".jpg", 0.78f);
    }

    private BufferedImage scaleToFit(BufferedImage source, int maxWidth, int maxHeight, boolean preserveAlpha) {
        double scale = Math.min(1d, Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int type = preserveAlpha || source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(width, height, type);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return output;
    }

    private BufferedImage centerOnWhiteSquare(BufferedImage image, int size) {
        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = square.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, size, size);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(image, (size - image.getWidth()) / 2, (size - image.getHeight()) / 2, null);
        graphics.dispose();
        return square;
    }

    private void writeNormalizedImage(NormalizedImage image, Path target) throws IOException {
        try {
            if (".png".equals(image.extension())) {
                writePng(image.image(), target);
            } else {
                writeJpeg(image.image(), image.jpegQuality(), target);
            }
        } finally {
            image.image().flush();
        }
    }

    private void writeNormalizedImageAtomically(NormalizedImage image, Path target, String prefix) throws IOException {
        Path tempFile = Files.createTempFile(target.getParent(), prefix + "-", image.extension() + ".tmp");
        try {
            writeNormalizedImage(image, tempFile);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void writePng(BufferedImage image, Path target) throws IOException {
        try (OutputStream output = Files.newOutputStream(target)) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("PNG encoder is unavailable.");
            }
        }
    }

    private void writeJpeg(BufferedImage image, float quality, Path target) throws IOException {
        BufferedImage rgb = image.getType() == BufferedImage.TYPE_INT_RGB ? image : scaleToFit(image, image.getWidth(), image.getHeight(), false);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (OutputStream output = Files.newOutputStream(target);
             ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(quality);
            writer.write(null, new IIOImage(rgb, null, null), parameters);
        } finally {
            writer.dispose();
            if (rgb != image) {
                rgb.flush();
            }
        }
    }

    private record NormalizedImage(BufferedImage image, String extension, float jpegQuality) {
    }
}
