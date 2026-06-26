package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWorkDTO {
    private UUID id;
    private UUID clientId;
    private String clientName;

    private String description;
    private Double amount;
    private Double rate;
    private Integer quantity;
    private Boolean billed;
    private Boolean isDeleted;
    private UUID invoiceId;
    private String previousInvoiceNumber;
    private LocalDateTime lastBilledDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime workDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}
