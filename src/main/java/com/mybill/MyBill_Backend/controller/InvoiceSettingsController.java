package com.mybill.MyBill_Backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.dto.InvoiceSettingsRequest;
import com.mybill.MyBill_Backend.entity.InvoiceSettings;
import com.mybill.MyBill_Backend.service.InvoiceSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

@RestController
@RequestMapping("/api/invoice/settings")
public class InvoiceSettingsController {

    private final InvoiceSettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public InvoiceSettingsController(InvoiceSettingsService settingsService, ObjectMapper objectMapper, Validator validator) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping
    public ResponseEntity<InvoiceSettings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvoiceSettings> updateSettings(@Valid @RequestBody InvoiceSettingsRequest settings) {
        return ResponseEntity.ok(settingsService.saveOrUpdateSettings(settings));
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<InvoiceSettings> updateSettingsFromText(@RequestBody String body) {
        try {
            InvoiceSettingsRequest settings =
                    objectMapper.readValue(body, InvoiceSettingsRequest.class);
            if (!validator.validate(settings).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice settings payload is invalid");
            }
            return ResponseEntity.ok(settingsService.saveOrUpdateSettings(settings));
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invoice settings payload is invalid",
                    ex
            );
        }
    }
}
