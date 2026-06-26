package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.BackupRequest;
import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    public ResponseEntity<BackupJob> createBackup(@RequestBody(required = false) BackupRequest request) {
        return ResponseEntity.ok(backupService.createBackup(request));
    }

    @GetMapping
    public ResponseEntity<List<BackupJob>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }
}
