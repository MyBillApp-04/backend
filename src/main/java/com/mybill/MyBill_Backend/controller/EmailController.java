package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.EmailTemplateRequest;
import com.mybill.MyBill_Backend.dto.SendEmailRequest;
import com.mybill.MyBill_Backend.entity.EmailLog;
import com.mybill.MyBill_Backend.entity.EmailStatus;
import com.mybill.MyBill_Backend.entity.EmailTemplate;
import com.mybill.MyBill_Backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplate>> listTemplates() {
        return ResponseEntity.ok(emailService.listTemplates());
    }

    @PostMapping("/templates")
    public ResponseEntity<EmailTemplate> createTemplate(@RequestBody EmailTemplateRequest request) {
        return ResponseEntity.ok(emailService.createTemplate(request));
    }

    @PutMapping("/templates/{templateId}")
    public ResponseEntity<EmailTemplate> updateTemplate(
            @PathVariable UUID templateId,
            @RequestBody EmailTemplateRequest request
    ) {
        return ResponseEntity.ok(emailService.updateTemplate(templateId, request));
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        emailService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    public ResponseEntity<EmailLog> sendEmail(@RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(emailService.sendEmail(request));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<EmailLog>> logs() {
        return ResponseEntity.ok(emailService.getLogs());
    }

    @GetMapping("/logs/page")
    public ResponseEntity<Page<EmailLog>> pagedLogs(
            @RequestParam(required = false) EmailStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(emailService.getLogs(status, pageable));
    }

    @GetMapping("/logs/{emailLogId}")
    public ResponseEntity<EmailLog> log(@PathVariable UUID emailLogId) {
        return ResponseEntity.ok(emailService.getLog(emailLogId));
    }
}
