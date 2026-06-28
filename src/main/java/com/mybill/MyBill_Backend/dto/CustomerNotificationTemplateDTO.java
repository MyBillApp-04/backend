package com.mybill.MyBill_Backend.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNotificationTemplateDTO {
    private UUID id;
    private String templateType;
    private String channel;
    private String subject;
    private String messageBody;
    private Boolean isCustomized;
}
