package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ReportSummaryDTO;
import com.mybill.MyBill_Backend.dto.AdvancedReportDTO;
import com.mybill.MyBill_Backend.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDTO> summary(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(reportingService.summary(year));
    }

    @GetMapping("/revenue-trends")
    public ResponseEntity<List<Map<String, Object>>> revenueTrends(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportingService.revenueTrends(year));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(reportingService.analytics(year));
    }

    @GetMapping("/advanced")
    public ResponseEntity<AdvancedReportDTO> advancedReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(reportingService.advancedReport(year, startDate, endDate));
    }
}
