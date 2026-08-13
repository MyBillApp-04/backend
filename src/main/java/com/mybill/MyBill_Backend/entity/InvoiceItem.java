package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_items_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_invoice_items_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_invoice_items_invoice", columnList = "invoice_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private ClientWork work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String description;
    private String dimension;
    @Column(columnDefinition = "double precision")
    private Double kgs;
    @Column(columnDefinition = "double precision")
    private Double rate;
    private Integer quantity;
    @Column(columnDefinition = "double precision")
    private Double amount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

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

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.quantity != null && this.rate != null) {
            this.amount = this.quantity * this.rate;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
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
        if (this.quantity != null && this.rate != null) {
            this.amount = this.quantity * this.rate;
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
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public ClientWork getWork() { return work; }
    public void setWork(ClientWork work) { this.work = work; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public Double getKgs() { return kgs; }
    public void setKgs(Double kgs) { this.kgs = kgs; }
    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
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
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public static InvoiceItemBuilder builder() { return new InvoiceItemBuilder(); }

    public static class InvoiceItemBuilder {
        private UUID id;
        private Invoice invoice;
        private ClientWork work;
        private User user;
        private String description;
        private String dimension;
        private Double kgs;
        private Double rate;
        private Integer quantity;
        private Double amount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean isDeleted = false;
        private String deviceId;
        private Integer version = 1;

        public InvoiceItemBuilder id(UUID id) { this.id = id; return this; }
        public InvoiceItemBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }
        public InvoiceItemBuilder work(ClientWork work) { this.work = work; return this; }
        public InvoiceItemBuilder user(User user) { this.user = user; return this; }
        public InvoiceItemBuilder description(String description) { this.description = description; return this; }
        public InvoiceItemBuilder dimension(String dimension) { this.dimension = dimension; return this; }
        public InvoiceItemBuilder kgs(Double kgs) { this.kgs = kgs; return this; }
        public InvoiceItemBuilder rate(Double rate) { this.rate = rate; return this; }
        public InvoiceItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public InvoiceItemBuilder amount(Double amount) { this.amount = amount; return this; }
        public InvoiceItemBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public InvoiceItemBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public InvoiceItemBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public InvoiceItemBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public InvoiceItemBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public InvoiceItemBuilder version(Integer version) { this.version = version; return this; }

        public InvoiceItem build() {
            InvoiceItem item = new InvoiceItem();
            item.id = this.id;
            item.invoice = this.invoice;
            item.work = this.work;
            item.user = this.user;
            item.description = this.description;
            item.dimension = this.dimension;
            item.kgs = this.kgs;
            item.rate = this.rate;
            item.quantity = this.quantity;
            item.amount = this.amount;
            item.createdAt = this.createdAt;
            item.updatedAt = this.updatedAt;
            item.deletedAt = this.deletedAt;
            item.isDeleted = this.isDeleted;
            item.deviceId = this.deviceId;
            item.version = this.version;
            return item;
        }
    }
}
