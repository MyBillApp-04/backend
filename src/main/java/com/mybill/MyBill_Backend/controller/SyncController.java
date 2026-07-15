package com.mybill.MyBill_Backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.sync.SyncRequest;
import com.mybill.MyBill_Backend.dto.sync.SyncResponse;
import com.mybill.MyBill_Backend.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${app.sync.max-decompressed-bytes:5242880}")
    private long maxDecompressedBytes;

    @PostMapping
    public ResponseEntity<SyncResponse> sync(HttpServletRequest request) throws IOException {
        InputStream inputStream = request.getInputStream();
        String encoding = request.getHeader("Content-Encoding");

        // Automatically handle GZIP compressed payloads from the mobile client
        boolean gzipped = encoding != null && encoding.toLowerCase().contains("gzip");
        if (encoding != null && !encoding.isBlank() && !gzipped && !"identity".equalsIgnoreCase(encoding)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content encoding");
        }

        if (gzipped) {
            try {
                inputStream = new GZIPInputStream(inputStream);
            } catch (ZipException e) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid gzip request body", e);
            }
        }

        inputStream = new BoundedInputStream(inputStream, maxDecompressedBytes);

        SyncRequest syncRequest;
        try {
            syncRequest = objectMapper.readValue(inputStream, SyncRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sync JSON", e);
        } catch (PayloadTooLargeException e) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Sync request body is too large", e);
        } catch (IOException e) {
            HttpStatus status = gzipped ? HttpStatus.UNSUPPORTED_MEDIA_TYPE : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status, "Unable to read sync request body", e);
        }
        validateSyncRequest(syncRequest);
        return ResponseEntity.ok(syncService.sync(syncRequest));
    }

    @PostMapping("/background")
    public ResponseEntity<SyncResponse> backgroundSync(@Valid @RequestBody SyncRequest syncRequest) {
        syncRequest.setBackground(true);
        return ResponseEntity.ok(syncService.sync(syncRequest));
    }

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<Map<String, Object>> deviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(syncService.getDeviceSyncStatus(deviceId));
    }

    private void validateSyncRequest(SyncRequest syncRequest) {
        if (syncRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sync request body is required");
        }
        Set<ConstraintViolation<SyncRequest>> violations = validator.validate(syncRequest);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead;

        private BoundedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = Math.max(1, maxBytes);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = super.read(b, off, len);
            if (count > 0) {
                count(count);
            }
            return count;
        }

        private void count(int count) throws PayloadTooLargeException {
            bytesRead += count;
            if (bytesRead > maxBytes) {
                throw new PayloadTooLargeException();
            }
        }
    }

    private static final class PayloadTooLargeException extends IOException {
    }
}
