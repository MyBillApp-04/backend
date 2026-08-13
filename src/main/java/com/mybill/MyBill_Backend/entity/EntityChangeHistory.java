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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }

    public String getChangeDetails() { return changeDetails; }
    public void setChangeDetails(String changeDetails) { this.changeDetails = changeDetails; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static EntityChangeHistoryBuilder builder() { return new EntityChangeHistoryBuilder(); }

    public static class EntityChangeHistoryBuilder {
        private UUID id;
        private String entityName;
        private UUID entityId;
        private String action;
        private Long changedBy;
        private String changeDetails;
        private LocalDateTime timestamp;

        public EntityChangeHistoryBuilder id(UUID id) { this.id = id; return this; }
        public EntityChangeHistoryBuilder entityName(String entityName) { this.entityName = entityName; return this; }
        public EntityChangeHistoryBuilder entityId(UUID entityId) { this.entityId = entityId; return this; }
        public EntityChangeHistoryBuilder action(String action) { this.action = action; return this; }
        public EntityChangeHistoryBuilder changedBy(Long changedBy) { this.changedBy = changedBy; return this; }
        public EntityChangeHistoryBuilder changeDetails(String changeDetails) { this.changeDetails = changeDetails; return this; }
        public EntityChangeHistoryBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public EntityChangeHistory build() {
            EntityChangeHistory h = new EntityChangeHistory();
            h.id = this.id;
            h.entityName = this.entityName;
            h.entityId = this.entityId;
            h.action = this.action;
            h.changedBy = this.changedBy;
            h.changeDetails = this.changeDetails;
            h.timestamp = this.timestamp;
            return h;
        }
    }
}
