package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    @Query("""
           SELECT t
           FROM EmailTemplate t
           WHERE t.isDeleted = false
             AND (t.user.id = :userId OR t.user IS NULL)
           ORDER BY t.templateType ASC, t.createdAt DESC
           """)
    List<EmailTemplate> findAvailableTemplates(@Param("userId") Long userId);

    Optional<EmailTemplate> findFirstByUserIdAndTemplateTypeAndIsDeletedFalse(Long userId, String templateType);
    Optional<EmailTemplate> findFirstByUserIsNullAndTemplateTypeAndIsDeletedFalse(String templateType);
    Optional<EmailTemplate> findByTemplateIdAndUserId(UUID templateId, Long userId);
}
