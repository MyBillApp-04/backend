package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entity_change_history", indexes = {
        @Index(name = "idx_entity_change_history_entity", columnList = "entity_name, entity_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityChangeHistory {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(nullable = false, length = 30)
    private String action;

    @CreatedBy
    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "change_details", columnDefinition = "text")
    private String changeDetails;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
