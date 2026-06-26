package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.DashboardStatsDTO;
import com.mybill.MyBill_Backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
// Fixed 404 deployment issue by explicitly allowing both base and trailing slash path formats
@RequestMapping({"/api/dashboard", "/api/dashboard/"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Fixed trailing slash matching issue introduced by Spring Boot 3 strict routing rules
    @GetMapping({"/summary", "/summary/"})
    public ResponseEntity<DashboardStatsDTO> getDashboardSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardSummary(month, year));
    }
}