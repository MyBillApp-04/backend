package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.ClientLedgerEntry;
import com.mybill.MyBill_Backend.entity.LedgerDirection;
import com.mybill.MyBill_Backend.entity.LedgerEntryType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLedgerEntryDTO {
    private UUID id;
    private UUID clientId;
    private UUID invoiceId;
    private UUID paymentId;
    private String invoiceNumber;
    private LedgerEntryType type;
    private LedgerDirection direction;
    private Double amount;
    private Double balanceAfter;
    private String notes;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
    private String deviceId;
    private Integer version;

    public static ClientLedgerEntryDTO fromEntity(ClientLedgerEntry entry) {
        return ClientLedgerEntryDTO.builder()
                .id(entry.getId())
                .clientId(entry.getClient() != null ? entry.getClient().getId() : null)
                .invoiceId(entry.getInvoice() != null ? entry.getInvoice().getId() : null)
                .paymentId(entry.getPayment() != null ? entry.getPayment().getPaymentId() : null)
                .invoiceNumber(entry.getInvoice() != null ? entry.getInvoice().getInvoiceNumber() : null)
                .type(entry.getType())
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .notes(entry.getNotes())
                .transactionDate(entry.getTransactionDate())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .deletedAt(entry.getDeletedAt())
                .isDeleted(entry.getIsDeleted())
                .deviceId(entry.getDeviceId())
                .version(entry.getVersion())
                .build();
    }
}
