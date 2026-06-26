package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.service.InvoiceSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice/settings")
@RequiredArgsConstructor
public class InvoiceSettingsController {

    private final InvoiceSettingsService settingsService;

    @GetMapping
    public ResponseEntity<InvoiceSettings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping
    public ResponseEntity<InvoiceSettings> updateSettings(@RequestBody InvoiceSettings settings) {
        return ResponseEntity.ok(settingsService.saveOrUpdateSettings(settings));
    }
}