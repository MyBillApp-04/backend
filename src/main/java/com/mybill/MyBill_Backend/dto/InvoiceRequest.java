package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.mybill.MyBill_Backend.entity.TaxType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {
    @NotNull(message = "Client is required")
    private UUID clientId;

    @NotEmpty(message = "Select at least one work item")
    private List<UUID> workIds;

    @PastOrPresent(message = "Invoice date cannot be in the future")
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "999999999999.99", message = "Discount is too large")
    @Digits(integer = 12, fraction = 2, message = "Discount can have at most 2 decimal places")
    private Double discount;

    @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
    private String notes;

    /** Applicable GST rate (0 = no tax). Supported: 0, 5, 12, 18, 28. */
    @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax rate is too large")
    private Double taxRate;

    /**
     * Optional explicit GST type override. When omitted, the service auto-resolves
     * INTRA/INTER from business and customer states.
     */
    private TaxType gstType;

    @AssertTrue(message = "Due date must be on or after invoice date")
    public boolean isDueDateOnOrAfterInvoiceDate() {
        return invoiceDate == null || dueDate == null || !dueDate.isBefore(invoiceDate);
    }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public List<UUID> getWorkIds() { return workIds; }
    public void setWorkIds(List<UUID> workIds) { this.workIds = workIds; }

    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }

    public TaxType getGstType() { return gstType; }
    public void setGstType(TaxType gstType) { this.gstType = gstType; }
}
