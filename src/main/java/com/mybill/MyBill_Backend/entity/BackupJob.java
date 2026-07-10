package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_jobs", indexes = {
        @Index(name = "idx_backup_jobs_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupJob {

    @Id
    @Column(name = "backup_id", columnDefinition = "uuid")
    private UUID backupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    private BackupProvider provider;y
    

    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    private String location;

    @Column(length = 64)
    private String sha256;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (backupId == null) backupId = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = BackupStatus.REQUESTED;
    }
}
