package com.mybill.MyBill_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_templates", indexes = {
        @Index(name = "idx_email_templates_user_type", columnList = "user_id, template_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @Column(name = "template_id", columnDefinition = "uuid")
    private UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "template_type", nullable = false)
    private String templateType;

    @Column(nullable = false)
    private String subject;

    @Column(name = "html_body", columnDefinition = "text", nullable = false)
    private String htmlBody;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (templateId == null) templateId = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isDeleted == null) isDeleted = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
