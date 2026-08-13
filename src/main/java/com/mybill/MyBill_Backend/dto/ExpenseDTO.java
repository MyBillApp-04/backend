package com.mybill.MyBill_Backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class ExpenseDTO {
    private UUID id;

    @NotBlank(message = "Expense description is required")
    @Size(max = 250, message = "Expense description must be 250 characters or fewer")
    private String description;

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Expense amount can have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Expense category is required")
    @Size(max = 80, message = "Expense category must be 80 characters or fewer")
    private String category;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @Size(max = 160, message = "Vendor name must be 160 characters or fewer")
    private String vendorName;

    @DecimalMin(value = "0.00", message = "Tax amount cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Tax amount can have at most 2 decimal places")
    private BigDecimal taxAmount;

    @Size(max = 500, message = "Receipt URL must be 500 characters or fewer")
    private String receiptUrl;

    private Boolean isRecurring;

    @Size(max = 50, message = "Recurring cycle must be 50 characters or fewer")
    private String recurringCycle;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    private Integer version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }
    public String getRecurringCycle() { return recurringCycle; }
    public void setRecurringCycle(String recurringCycle) { this.recurringCycle = recurringCycle; }
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

    public static ExpenseDTOBuilder builder() { return new ExpenseDTOBuilder(); }

    public static class ExpenseDTOBuilder {
        private UUID id;
        private String description;
        private BigDecimal amount;
        private String category;
        private LocalDate expenseDate;
        private String vendorName;
        private BigDecimal taxAmount;
        private String receiptUrl;
        private Boolean isRecurring;
        private String recurringCycle;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean isDeleted;
        private LocalDateTime deletedAt;
        private Integer version;

        public ExpenseDTOBuilder id(UUID id) { this.id = id; return this; }
        public ExpenseDTOBuilder description(String description) { this.description = description; return this; }
        public ExpenseDTOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public ExpenseDTOBuilder category(String category) { this.category = category; return this; }
        public ExpenseDTOBuilder expenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; return this; }
        public ExpenseDTOBuilder vendorName(String vendorName) { this.vendorName = vendorName; return this; }
        public ExpenseDTOBuilder taxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; return this; }
        public ExpenseDTOBuilder receiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; return this; }
        public ExpenseDTOBuilder isRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; return this; }
        public ExpenseDTOBuilder recurringCycle(String recurringCycle) { this.recurringCycle = recurringCycle; return this; }
        public ExpenseDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ExpenseDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ExpenseDTOBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public ExpenseDTOBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public ExpenseDTOBuilder version(Integer version) { this.version = version; return this; }

        public ExpenseDTO build() {
            ExpenseDTO dto = new ExpenseDTO();
            dto.id = this.id;
            dto.description = this.description;
            dto.amount = this.amount;
            dto.category = this.category;
            dto.expenseDate = this.expenseDate;
            dto.vendorName = this.vendorName;
            dto.taxAmount = this.taxAmount;
            dto.receiptUrl = this.receiptUrl;
            dto.isRecurring = this.isRecurring;
            dto.recurringCycle = this.recurringCycle;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.isDeleted = this.isDeleted;
            dto.deletedAt = this.deletedAt;
            dto.version = this.version;
            return dto;
        }
    }
}
