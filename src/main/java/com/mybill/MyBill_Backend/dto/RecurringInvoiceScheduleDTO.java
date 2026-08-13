package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceScheduleDTO {
    private UUID id;

    @NotNull(message = "Client is required")
    private UUID clientId;
    private String clientName;

    @NotBlank(message = "Schedule description is required")
    @Size(max = 250, message = "Schedule description must be 250 characters or fewer")
    private String description;

    @NotNull(message = "Schedule amount is required")
    @DecimalMin(value = "0.01", message = "Schedule amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Schedule amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Billing cycle is required")
    private String billingCycle;
    private String cronExpression;
    private String status;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRunDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRunDate;

    private Boolean autoCharge;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
    
    private Integer version;

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isEndDateOnOrAfterStartDate() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public static RecurringInvoiceScheduleDTOBuilder builder() { return new RecurringInvoiceScheduleDTOBuilder(); }

    public static class RecurringInvoiceScheduleDTOBuilder {
        private UUID id;
        private UUID clientId;
        private String clientName;
        private String description;
        private BigDecimal amount;
        private String billingCycle;
        private String cronExpression;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime nextRunDate;
        private LocalDateTime lastRunDate;
        private Boolean autoCharge;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean isDeleted;
        private LocalDateTime deletedAt;
        private Integer version;

        public RecurringInvoiceScheduleDTOBuilder id(UUID id) { this.id = id; return this; }
        public RecurringInvoiceScheduleDTOBuilder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public RecurringInvoiceScheduleDTOBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public RecurringInvoiceScheduleDTOBuilder description(String description) { this.description = description; return this; }
        public RecurringInvoiceScheduleDTOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public RecurringInvoiceScheduleDTOBuilder billingCycle(String billingCycle) { this.billingCycle = billingCycle; return this; }
        public RecurringInvoiceScheduleDTOBuilder cronExpression(String cronExpression) { this.cronExpression = cronExpression; return this; }
        public RecurringInvoiceScheduleDTOBuilder status(String status) { this.status = status; return this; }
        public RecurringInvoiceScheduleDTOBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public RecurringInvoiceScheduleDTOBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public RecurringInvoiceScheduleDTOBuilder nextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; return this; }
        public RecurringInvoiceScheduleDTOBuilder lastRunDate(LocalDateTime lastRunDate) { this.lastRunDate = lastRunDate; return this; }
        public RecurringInvoiceScheduleDTOBuilder autoCharge(Boolean autoCharge) { this.autoCharge = autoCharge; return this; }
        public RecurringInvoiceScheduleDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RecurringInvoiceScheduleDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RecurringInvoiceScheduleDTOBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public RecurringInvoiceScheduleDTOBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public RecurringInvoiceScheduleDTOBuilder version(Integer version) { this.version = version; return this; }

        public RecurringInvoiceScheduleDTO build() {
            RecurringInvoiceScheduleDTO dto = new RecurringInvoiceScheduleDTO();
            dto.id = this.id;
            dto.clientId = this.clientId;
            dto.clientName = this.clientName;
            dto.description = this.description;
            dto.amount = this.amount;
            dto.billingCycle = this.billingCycle;
            dto.cronExpression = this.cronExpression;
            dto.status = this.status;
            dto.startDate = this.startDate;
            dto.endDate = this.endDate;
            dto.nextRunDate = this.nextRunDate;
            dto.lastRunDate = this.lastRunDate;
            dto.autoCharge = this.autoCharge;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.isDeleted = this.isDeleted;
            dto.deletedAt = this.deletedAt;
            dto.version = this.version;
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

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; }

    public LocalDateTime getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(LocalDateTime lastRunDate) { this.lastRunDate = lastRunDate; }

    public Boolean getAutoCharge() { return autoCharge; }
    public void setAutoCharge(Boolean autoCharge) { this.autoCharge = autoCharge; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
