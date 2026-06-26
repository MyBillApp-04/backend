package com.mybill.MyBill_Backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health-check endpoint for UptimeRobot.
 *
 * Configure UptimeRobot to monitor:
 *   URL  : https://mybill-backend-vckc.onrender.com/ping
 *   Type : HTTP(s)
 *   Interval: 5 minutes
 *
 * This keeps the Render free-tier instance warm so it never cold-starts
 * during normal business hours. UptimeRobot pings every 5 minutes;
 * Render sleeps after 15 minutes of inactivity — so 5-minute pings
 * guarantee the server is always awake.
 *
 * No authentication required — SecurityConfig explicitly permits /ping.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "MyBill Backend",
                "timestamp", Instant.now().toString()
        ));
    }
}