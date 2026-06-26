package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ReportSummaryDTO;
import com.mybill.MyBill_Backend.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
