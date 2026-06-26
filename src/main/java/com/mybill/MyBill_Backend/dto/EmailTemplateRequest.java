package com.mybill.MyBill_Backend.dto;

import lombok.Data;

@Data
public class EmailTemplateRequest {
    private String templateType;
    private String subject;
    private String htmlBody;
}
