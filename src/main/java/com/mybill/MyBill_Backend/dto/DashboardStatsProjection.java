package com.mybill.MyBill_Backend.dto;

public interface DashboardStatsProjection {

    Long getTotalClients();

    Double getThisMonthBilled();

    Double getThisMonthReceived();

    Double getTotalPending();

    Long getPendingInvoices();

    String getTopClient();
}