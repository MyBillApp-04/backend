package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Notification;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.NotificationRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, boundedPageable(pageable));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return notificationRepository.findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(
                userId,
                boundedPageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(securityUtils.getCurrentUserId());
    }

    public Notification createNotification(String title, String message) {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID())
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    public Notification markAsRead(UUID notificationId) {
        Long userId = securityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        Long userId = securityUtils.getCurrentUserId();
        notificationRepository.markUnreadAsReadForUser(userId, LocalDateTime.now());
    }

    public void deleteNotification(UUID notificationId) {
        Long userId = securityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        notification.setIsDeleted(true);
        notification.setDeletedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private Pageable boundedPageable(Pageable pageable) {
        int page = pageable != null && pageable.isPaged() ? Math.max(0, pageable.getPageNumber()) : 0;
        int size = pageable != null && pageable.isPaged() ? pageable.getPageSize() : 50;
        return PageRequest.of(page, Math.max(1, Math.min(size, 100)));
    }
}
