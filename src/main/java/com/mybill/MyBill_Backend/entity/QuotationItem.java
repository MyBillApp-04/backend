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
@Table(name = "quotation_items", indexes = {
        @Index(name = "idx_quotation_items_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_quotation_items_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_quotation_items_quotation", columnList = "quotation_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonIgnore
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String description;

    private String dimension;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "double precision")
    private Double kgs;

    @Column(columnDefinition = "double precision", nullable = false)
    private Double amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "device_id")
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

    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getKgs() { return kgs; }
    public void setKgs(Double kgs) { this.kgs = kgs; }

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

    public static QuotationItemBuilder builder() { return new QuotationItemBuilder(); }

    public static class QuotationItemBuilder {
        private UUID id;
        private Quotation quotation;
        private User user;
        private String description;
        private String dimension;
        private Integer quantity;
        private Double kgs;
        private Double amount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private Boolean isDeleted = false;
        private String deviceId;
        private Integer version = 1;

        public QuotationItemBuilder id(UUID id) { this.id = id; return this; }
        public QuotationItemBuilder quotation(Quotation quotation) { this.quotation = quotation; return this; }
        public QuotationItemBuilder user(User user) { this.user = user; return this; }
        public QuotationItemBuilder description(String description) { this.description = description; return this; }
        public QuotationItemBuilder dimension(String dimension) { this.dimension = dimension; return this; }
        public QuotationItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public QuotationItemBuilder kgs(Double kgs) { this.kgs = kgs; return this; }
        public QuotationItemBuilder amount(Double amount) { this.amount = amount; return this; }
        public QuotationItemBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public QuotationItemBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public QuotationItemBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public QuotationItemBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }
        public QuotationItemBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public QuotationItemBuilder version(Integer version) { this.version = version; return this; }

        public QuotationItem build() {
            QuotationItem item = new QuotationItem();
            item.id = this.id;
            item.quotation = this.quotation;
            item.user = this.user;
            item.description = this.description;
            item.dimension = this.dimension;
            item.quantity = this.quantity;
            item.kgs = this.kgs;
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
