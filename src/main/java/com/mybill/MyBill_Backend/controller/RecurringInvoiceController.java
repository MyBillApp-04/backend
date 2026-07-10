package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.RecurringInvoiceScheduleDTO;
import com.mybill.MyBill_Backend.entity.RecurringInvoiceSchedule;
import com.mybill.MyBill_Backend.service.RecurringInvoiceScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringInvoiceController {

    private final RecurringInvoiceScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<RecurringInvoiceScheduleDTO>> getSchedules() {
        List<RecurringInvoiceScheduleDTO> dtos = scheduleService.getSchedulesForUser()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringInvoiceScheduleDTO> getSchedule(@PathVariable UUID id) {
        RecurringInvoiceSchedule schedule = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(toDTO(schedule));
    }

    @PostMapping
    public ResponseEntity<RecurringInvoiceScheduleDTO> createSchedule(
            @Valid @RequestBody RecurringInvoiceScheduleDTO dto
    ) {
        RecurringInvoiceSchedule created = scheduleService.createSchedule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringInvoiceScheduleDTO> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody RecurringInvoiceScheduleDTO dto
    ) {
        RecurringInvoiceSchedule updated = scheduleService.updateSchedule(id, dto);
        return ResponseEntity.ok(toDTO(updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RecurringInvoiceScheduleDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        RecurringInvoiceSchedule updated = scheduleService.setScheduleStatus(id, status);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    private RecurringInvoiceScheduleDTO toDTO(RecurringInvoiceSchedule schedule) {
        return RecurringInvoiceScheduleDTO.builder()
                .id(schedule.getId())
                .clientId(schedule.getClient().getId())
                .clientName(schedule.getClient().getName())
                .description(schedule.getDescription())
                .amount(schedule.getAmount())
                .billingCycle(schedule.getBillingCycle())
                .cronExpression(schedule.getCronExpression())
                .status(schedule.getStatus())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .nextRunDate(schedule.getNextRunDate())
                .lastRunDate(schedule.getLastRunDate())
                .autoCharge(schedule.getAutoCharge())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .isDeleted(schedule.getIsDeleted())
                .deletedAt(schedule.getDeletedAt())
                .version(schedule.getVersion())
                .build();
    }
}
