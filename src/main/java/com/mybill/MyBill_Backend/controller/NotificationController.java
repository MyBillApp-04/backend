package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.Notification;
import com.mybill.MyBill_Backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<Notification>> getUnreadNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(pageable));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestBody Map<String, String> body
    ) {
        String title = body.get("title");
        String message = body.get("message");

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Notification notification = notificationService.createNotification(title, message);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
