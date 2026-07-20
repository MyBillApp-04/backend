package com.mybill.MyBill_Backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GlobalSearchResponse {
    private List<Map<String, Object>> clients;
    private List<Map<String, Object>> works;
    private List<Map<String, Object>> invoices;
    private List<Map<String, Object>> quotations;
}
