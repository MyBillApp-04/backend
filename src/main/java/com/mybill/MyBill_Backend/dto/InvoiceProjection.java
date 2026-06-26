package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InvoiceProjection {
    UUID getId();
    String getInvoiceNumber();
    Double getSubtotal();
    Double getDiscount();
    Double getGrossAmount();
    Double getAdvanceApplied();
    Double getNetPayable();
    Double getTotalAmount();
    Double getPaidAmount();
    Double getPendingAmount();
    PaymentStatus getPaymentStatus();
    LocalDateTime getInvoiceDate();
    LocalDateTime getDueDate();
    String getNotes();
    ClientSummary getClient();

    interface ClientSummary {
        UUID getId();
        String getName();
    }
}
