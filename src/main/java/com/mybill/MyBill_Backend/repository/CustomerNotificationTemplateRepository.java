package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.CustomerNotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerNotificationTemplateRepository extends JpaRepository<CustomerNotificationTemplate, UUID> {
    Optional<CustomerNotificationTemplate> findByUserIdAndTemplateTypeAndChannelAndIsDeletedFalse(Long userId, String templateType, String channel);
    
    Optional<CustomerNotificationTemplate> findByUserIdIsNullAndTemplateTypeAndChannelAndIsDeletedFalse(String templateType, String channel);

    List<CustomerNotificationTemplate> findByUserIdAndIsDeletedFalse(Long userId);

    @Query("SELECT t FROM CustomerNotificationTemplate t WHERE (t.user.id = :userId OR t.user IS NULL) AND t.isDeleted = false")
    List<CustomerNotificationTemplate> findAvailableTemplates(Long userId);
}
