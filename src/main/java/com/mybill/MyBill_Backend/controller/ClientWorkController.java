package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ClientSummaryProjection;
import com.mybill.MyBill_Backend.dto.ClientWorkDTO;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.service.ClientWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<ClientWorkDTO> getClientWork(@PathVariable UUID clientId, Pageable pageable) {
        return workService.getClientWork(clientId, pageable);
    }

    @GetMapping("/all")
    public Page<ClientWorkDTO> getAllWork(Pageable pageable) {
        return workService.getAllWork(pageable);
    }

    @GetMapping("/unbilled/{clientId}")
    public Page<ClientWorkDTO> getUnbilledWorks(@PathVariable UUID clientId, Pageable pageable) {
        return workService.getUnbilledWorks(clientId, pageable);
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
            Pageable pageable
    ) {
        return workService.getWorkUpdatedSince(since, pageable).getContent();
    }
}
