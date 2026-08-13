package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotation_response_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationResponseEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Column(nullable = false, length = 30)
    private String action; // ACCEPTED, DECLINED, DISCUSSION_REQUESTED

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    @Column(name = "discussion_message", columnDefinition = "text")
    private String discussionMessage;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.respondedAt == null) {
            this.respondedAt = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public static QuotationResponseEventBuilder builder() { return new QuotationResponseEventBuilder(); }

    public static class QuotationResponseEventBuilder {
        private UUID id;
        private Quotation quotation;
        private String action;
        private LocalDateTime respondedAt;
        private String discussionMessage;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;

        public QuotationResponseEventBuilder id(UUID id) { this.id = id; return this; }
        public QuotationResponseEventBuilder quotation(Quotation quotation) { this.quotation = quotation; return this; }
        public QuotationResponseEventBuilder action(String action) { this.action = action; return this; }
        public QuotationResponseEventBuilder respondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; return this; }
        public QuotationResponseEventBuilder discussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; return this; }
        public QuotationResponseEventBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public QuotationResponseEventBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public QuotationResponseEventBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public QuotationResponseEvent build() {
            QuotationResponseEvent event = new QuotationResponseEvent();
            event.id = this.id;
            event.quotation = this.quotation;
            event.action = this.action;
            event.respondedAt = this.respondedAt;
            event.discussionMessage = this.discussionMessage;
            event.ipAddress = this.ipAddress;
            event.userAgent = this.userAgent;
            event.createdAt = this.createdAt;
            return event;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getDiscussionMessage() { return discussionMessage; }
    public void setDiscussionMessage(String discussionMessage) { this.discussionMessage = discussionMessage; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
