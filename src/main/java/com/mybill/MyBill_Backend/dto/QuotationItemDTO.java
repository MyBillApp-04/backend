package com.mybill.MyBill_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemDTO {
    private UUID id;
    private String description;
    private String dimension;
    private Integer quantity;
    private Double kgs;
    private Double amount;
    private Integer version;
}
