package com.mybill.MyBill_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    private long totalClients;
    private double thisMonthBilled;
    private double thisMonthReceived;
    private double totalPending;
    private long pendingInvoices;
    private String topClient;

    private List<ClientWorkDTO> recentActivity;
}