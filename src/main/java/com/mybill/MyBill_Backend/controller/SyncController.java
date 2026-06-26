package com.mybill.MyBill_Backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.sync.SyncRequest;
import com.mybill.MyBill_Backend.dto.sync.SyncResponse;
import com.mybill.MyBill_Backend.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final ObjectMapper objectMapper;

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

        SyncRequest syncRequest;
        try {
            syncRequest = objectMapper.readValue(inputStream, SyncRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sync JSON", e);
        } catch (IOException e) {
            HttpStatus status = gzipped ? HttpStatus.UNSUPPORTED_MEDIA_TYPE : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status, "Unable to read sync request body", e);
        }
        return ResponseEntity.ok(syncService.sync(syncRequest));
    }

    @PostMapping("/background")
    public ResponseEntity<SyncResponse> backgroundSync(@RequestBody SyncRequest syncRequest) {
        syncRequest.setBackground(true);
        return ResponseEntity.ok(syncService.sync(syncRequest));
    }

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<Map<String, Object>> deviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(syncService.getDeviceSyncStatus(deviceId));
    }
}
