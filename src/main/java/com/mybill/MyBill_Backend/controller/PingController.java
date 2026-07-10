package com.mybill.MyBill_Backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public liveness API used by uptime monitoring and performance smoke tests.
 *
 * <p>No authentication is required. A successful response indicates that the
 * web process can serve requests; it does not guarantee database readiness.</p>
 */
@RestController
public class PingController {

    /**
     * Returns process liveness and the server timestamp.
     *
     * @return HTTP 200 with status, service name, and ISO-8601 timestamp
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "MyBill Backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
