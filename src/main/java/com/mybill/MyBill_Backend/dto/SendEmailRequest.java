package com.mybill.MyBill_Backend.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SendEmailRequest {
    private String to;
    private String templateType;
    private UUID invoiceId;
    private Map<String, Object> variables;
    private Boolean attachInvoicePdf;
}
