package com.mybill.MyBill_Backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportSummaryDTO {
    private Map<String, Object> revenue;
    private Map<String, Object> invoices;
    private Map<String, Object> clients;
    private List<Map<String, Object>> trends;
}
