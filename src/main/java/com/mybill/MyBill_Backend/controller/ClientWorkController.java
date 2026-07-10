package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ClientSummaryProjection;
import com.mybill.MyBill_Backend.dto.ClientWorkDTO;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.service.ClientWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
public class ClientWorkController {

    private final ClientWorkService workService;

    @PostMapping("/{clientId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientWork addWork(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientWork work
    ) {
        return workService.addWork(clientId, work);
    }

    @GetMapping("/client/{clientId}")
    public List<ClientWorkDTO> getClientWork(@PathVariable UUID clientId) {
        return workService.getClientWork(clientId);
    }

    @GetMapping("/all")
    public List<ClientWorkDTO> getAllWork() {
        return workService.getAllWork();
    }

    @GetMapping("/unbilled/{clientId}")
    public List<ClientWorkDTO> getUnbilledWorks(@PathVariable UUID clientId) {
        return workService.getUnbilledWorks(clientId);
    }

    @GetMapping("/total/{clientId}")
    public Double getTotalAmount(@PathVariable UUID clientId) {
        return workService.getTotalAmount(clientId);
    }

    @GetMapping("/summary")
    public List<ClientSummaryProjection> getClientSummary() {
        return workService.getClientSummary();
    }

    @PutMapping("/{workId}")
    public ClientWork updateWork(
            @PathVariable UUID workId,
            @Valid @RequestBody ClientWork work
    ) {
        return workService.updateWork(workId, work);
    }

    @DeleteMapping("/{workId}")
    public ResponseEntity<Object> deleteWork(@PathVariable UUID workId) {
        workService.deleteWork(workId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sync")
    public List<ClientWorkDTO> getWorkUpdatedSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    ) {
        return workService.getWorkUpdatedSince(since, pageable).getContent();
    }
}