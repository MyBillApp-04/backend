package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "catalog_items", indexes = {
        @Index(name = "idx_catalog_items_user_updated", columnList = "user_id, updated_at"),
        @Index(name = "idx_catalog_items_user_deleted", columnList = "user_id, is_deleted"),
        @Index(name = "idx_catalog_items_user_active", columnList = "user_id, is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogItem {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // PRODUCT, SERVICE

    @Column(name = "default_rate", columnDefinition = "double precision")
    private Double defaultRate;

    @Column(name = "default_tax_rate", columnDefinition = "double precision")
    private Double defaultTaxRate;

    private String unit;

    private String dimension;

    @Column(name = "kgs", columnDefinition = "double precision")
    private Double kgs;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    private String deviceId;

    @Version
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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}