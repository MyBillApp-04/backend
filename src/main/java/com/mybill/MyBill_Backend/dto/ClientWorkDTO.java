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

    public static ClientWorkDTOBuilder builder() { return new ClientWorkDTOBuilder(); }

    public static class ClientWorkDTOBuilder {
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
        private LocalDateTime workDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;

        public ClientWorkDTOBuilder id(UUID id) { this.id = id; return this; }
        public ClientWorkDTOBuilder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public ClientWorkDTOBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public ClientWorkDTOBuilder description(String description) { this.description = description; return this; }
        public ClientWorkDTOBuilder amount(Double amount) { this.amount = amount; return this; }
        public ClientWorkDTOBuilder rate(Double rate) { this.rate = rate; return this; }
        public ClientWorkDTOBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public ClientWorkDTOBuilder billed(Boolean billed) { this.billed = billed; return this; }
        public ClientWorkDTOBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public ClientWorkDTOBuilder invoiceId(UUID invoiceId) { this.invoiceId = invoiceId; return this; }
        public ClientWorkDTOBuilder previousInvoiceNumber(String previousInvoiceNumber) { this.previousInvoiceNumber = previousInvoiceNumber; return this; }
        public ClientWorkDTOBuilder lastBilledDate(LocalDateTime lastBilledDate) { this.lastBilledDate = lastBilledDate; return this; }
        public ClientWorkDTOBuilder workDate(LocalDateTime workDate) { this.workDate = workDate; return this; }
        public ClientWorkDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ClientWorkDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ClientWorkDTOBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }

        public ClientWorkDTO build() {
            ClientWorkDTO dto = new ClientWorkDTO();
            dto.id = this.id;
            dto.clientId = this.clientId;
            dto.clientName = this.clientName;
            dto.description = this.description;
            dto.amount = this.amount;
            dto.rate = this.rate;
            dto.quantity = this.quantity;
            dto.billed = this.billed;
            dto.isDeleted = this.isDeleted;
            dto.invoiceId = this.invoiceId;
            dto.previousInvoiceNumber = this.previousInvoiceNumber;
            dto.lastBilledDate = this.lastBilledDate;
            dto.workDate = this.workDate;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.deletedAt = this.deletedAt;
            return dto;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getBilled() { return billed; }
    public void setBilled(Boolean billed) { this.billed = billed; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public String getPreviousInvoiceNumber() { return previousInvoiceNumber; }
    public void setPreviousInvoiceNumber(String previousInvoiceNumber) { this.previousInvoiceNumber = previousInvoiceNumber; }

    public LocalDateTime getLastBilledDate() { return lastBilledDate; }
    public void setLastBilledDate(LocalDateTime lastBilledDate) { this.lastBilledDate = lastBilledDate; }

    public LocalDateTime getWorkDate() { return workDate; }
    public void setWorkDate(LocalDateTime workDate) { this.workDate = workDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
