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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LedgerEntryType getType() { return type; }
    public void setType(LedgerEntryType type) { this.type = type; }
    public LedgerDirection getDirection() { return direction; }
    public void setDirection(LedgerDirection direction) { this.direction = direction; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public static ClientLedgerEntryDTOBuilder builder() { return new ClientLedgerEntryDTOBuilder(); }

    public static class ClientLedgerEntryDTOBuilder {
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

        public ClientLedgerEntryDTOBuilder id(UUID id) { this.id = id; return this; }
        public ClientLedgerEntryDTOBuilder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public ClientLedgerEntryDTOBuilder invoiceId(UUID invoiceId) { this.invoiceId = invoiceId; return this; }
        public ClientLedgerEntryDTOBuilder paymentId(UUID paymentId) { this.paymentId = paymentId; return this; }
        public ClientLedgerEntryDTOBuilder invoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; return this; }
        public ClientLedgerEntryDTOBuilder type(LedgerEntryType type) { this.type = type; return this; }
        public ClientLedgerEntryDTOBuilder direction(LedgerDirection direction) { this.direction = direction; return this; }
        public ClientLedgerEntryDTOBuilder amount(Double amount) { this.amount = amount; return this; }
        public ClientLedgerEntryDTOBuilder balanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public ClientLedgerEntryDTOBuilder notes(String notes) { this.notes = notes; return this; }
        public ClientLedgerEntryDTOBuilder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public ClientLedgerEntryDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ClientLedgerEntryDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ClientLedgerEntryDTOBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public ClientLedgerEntryDTOBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public ClientLedgerEntryDTOBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public ClientLedgerEntryDTOBuilder version(Integer version) { this.version = version; return this; }

        public ClientLedgerEntryDTO build() {
            ClientLedgerEntryDTO dto = new ClientLedgerEntryDTO();
            dto.id = this.id;
            dto.clientId = this.clientId;
            dto.invoiceId = this.invoiceId;
            dto.paymentId = this.paymentId;
            dto.invoiceNumber = this.invoiceNumber;
            dto.type = this.type;
            dto.direction = this.direction;
            dto.amount = this.amount;
            dto.balanceAfter = this.balanceAfter;
            dto.notes = this.notes;
            dto.transactionDate = this.transactionDate;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.deletedAt = this.deletedAt;
            dto.isDeleted = this.isDeleted;
            dto.deviceId = this.deviceId;
            dto.version = this.version;
            return dto;
        }
    }
}
