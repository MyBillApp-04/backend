package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.mybill.MyBill_Backend.validation.TwoDecimalPlaces;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_work", indexes = {
        @Index(name = "idx_work_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_work_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_work_client", columnList = "client_id"),
        @Index(name = "idx_work_invoice", columnList = "invoice_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWork {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotBlank(message = "Work description is required")
    private String description;

    @NotNull(message = "Rate is required")
    @Positive(message = "Rate must be positive")
    @TwoDecimalPlaces(message = "Rate can have at most two decimal places")
    @Column(columnDefinition = "double precision")
    private Double rate;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Column(columnDefinition = "double precision")
    private Double amount;

    @PastOrPresent(message = "Date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean billed = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "previous_invoice_number")
    private String previousInvoiceNumber;

    @Column(name = "last_billed_date")
    private LocalDateTime lastBilledDate;

    private String deviceId;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonBackReference
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (quantity != null && rate != null) {
            this.amount = quantity * rate;
        }

        if (this.date == null) {
            this.date = now;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.billed == null) {
            this.billed = false;
        }

        if (this.isDeleted == null) {
            this.isDeleted = false;
        }

        if (this.version == null) {
            this.version = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (quantity != null && rate != null) {
            this.amount = quantity * rate;
        }

        this.updatedAt = LocalDateTime.now();

        if (this.version == null) {
            this.version = 1;
        } else {
            this.version++;
        }
    }

    public void markDeleted(LocalDateTime deletedAt) {
        LocalDateTime timestamp = deletedAt != null ? deletedAt : LocalDateTime.now();
        this.isDeleted = true;
        this.deletedAt = timestamp;
        this.updatedAt = timestamp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Boolean getBilled() { return billed; }
    public void setBilled(Boolean billed) { this.billed = billed; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getPreviousInvoiceNumber() { return previousInvoiceNumber; }
    public void setPreviousInvoiceNumber(String previousInvoiceNumber) { this.previousInvoiceNumber = previousInvoiceNumber; }
    public LocalDateTime getLastBilledDate() { return lastBilledDate; }
    public void setLastBilledDate(LocalDateTime lastBilledDate) { this.lastBilledDate = lastBilledDate; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public static ClientWorkBuilder builder() { return new ClientWorkBuilder(); }

    public static class ClientWorkBuilder {
        private UUID id;
        private Client client;
        private Invoice invoice;
        private User user;
        private String description;
        private Double rate;
        private Integer quantity;
        private Double amount;
        private LocalDateTime date;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean billed = false;
        private Boolean isDeleted = false;
        private String previousInvoiceNumber;
        private LocalDateTime lastBilledDate;
        private String deviceId;
        private Integer version = 1;

        public ClientWorkBuilder id(UUID id) { this.id = id; return this; }
        public ClientWorkBuilder client(Client client) { this.client = client; return this; }
        public ClientWorkBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public ClientWorkBuilder user(User user) { this.user = user; return this; }
        public ClientWorkBuilder description(String description) { this.description = description; return this; }
        public ClientWorkBuilder rate(Double rate) { this.rate = rate; return this; }
        public ClientWorkBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public ClientWorkBuilder amount(Double amount) { this.amount = amount; return this; }
        public ClientWorkBuilder date(LocalDateTime date) { this.date = date; return this; }
        public ClientWorkBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ClientWorkBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ClientWorkBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public ClientWorkBuilder billed(Boolean billed) { this.billed = billed; return this; }
        public ClientWorkBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public ClientWorkBuilder previousInvoiceNumber(String previousInvoiceNumber) { this.previousInvoiceNumber = previousInvoiceNumber; return this; }
        public ClientWorkBuilder lastBilledDate(LocalDateTime lastBilledDate) { this.lastBilledDate = lastBilledDate; return this; }
        public ClientWorkBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public ClientWorkBuilder version(Integer version) { this.version = version; return this; }

        public ClientWork build() {
            ClientWork work = new ClientWork();
            work.id = this.id;
            work.client = this.client;
            work.invoice = this.invoice;
            work.user = this.user;
            work.description = this.description;
            work.rate = this.rate;
            work.quantity = this.quantity;
            work.amount = this.amount;
            work.date = this.date;
            work.createdAt = this.createdAt;
            work.updatedAt = this.updatedAt;
            work.deletedAt = this.deletedAt;
            work.billed = this.billed;
            work.isDeleted = this.isDeleted;
            work.previousInvoiceNumber = this.previousInvoiceNumber;
            work.lastBilledDate = this.lastBilledDate;
            work.deviceId = this.deviceId;
            work.version = this.version;
            return work;
        }
    }
}
