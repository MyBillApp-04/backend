package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.entity.QuotationResponseEvent;
import com.mybill.MyBill_Backend.entity.QuotationStatus;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.repository.QuotationResponseEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuotationPublicResponseService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int DEFAULT_EXPIRY_DAYS = 30;

    private final QuotationRepository quotationRepository;
    private final QuotationResponseEventRepository responseEventRepository;
    private final FcmNotificationService fcmNotificationService;

    @Value("${app.public-url.base-url:https://mybill.onrender.com}")
    private String baseUrl;

    public QuotationPublicResponseService(
            QuotationRepository quotationRepository,
            QuotationResponseEventRepository responseEventRepository,
            FcmNotificationService fcmNotificationService) {
        this.quotationRepository = quotationRepository;
        this.responseEventRepository = responseEventRepository;
        this.fcmNotificationService = fcmNotificationService;
    }

    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String generateRandomToken() {
        byte[] randomBytes = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Transactional
    public PublicLinkResult generateOrRegeneratePublicLink(UUID quotationId, Long userId) {
        Quotation quotation = quotationRepository.findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found or unauthorized"));

        if (Boolean.TRUE.equals(quotation.getIsDeleted())) {
            throw new IllegalStateException("Cannot generate public link for deleted quotation");
        }

        String rawToken = generateRandomToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = quotation.getValidUntilDate() != null
                ? quotation.getValidUntilDate()
                : now.plusDays(DEFAULT_EXPIRY_DAYS);

        quotation.setPublicTokenHash(tokenHash);
        quotation.setTokenCreatedAt(now);
        quotation.setTokenExpiresAt(expiresAt);
        quotation.setTokenRevokedAt(null);
        if (quotation.getClientResponseStatus() == null) {
            quotation.setClientResponseStatus("PENDING");
        }

        quotationRepository.save(quotation);

        String fullUrl = normalizeBaseUrl(baseUrl) + "/q/" + rawToken;
        return new PublicLinkResult(rawToken, fullUrl, expiresAt);
    }

    @Transactional
    public void revokePublicLink(UUID quotationId, Long userId) {
        Quotation quotation = quotationRepository.findByIdAndUserId(quotationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found or unauthorized"));

        quotation.setTokenRevokedAt(LocalDateTime.now());
        quotationRepository.save(quotation);
    }

    @Transactional
    public PublicQuotationView getPublicQuotationView(String rawToken) {
        String tokenHash = hashToken(rawToken);
        if (tokenHash == null) {
            return PublicQuotationView.invalid("Invalid link token format.");
        }

        Optional<Quotation> optQuote = resolveQuotationByTokenOrId(rawToken, tokenHash);
        if (optQuote.isEmpty()) {
            return PublicQuotationView.invalid("Quotation link not found or invalid.");
        }

        Quotation q = optQuote.get();
        if (Boolean.TRUE.equals(q.getIsDeleted())) {
            return PublicQuotationView.invalid("Quotation is no longer available.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (q.getTokenRevokedAt() != null) {
            return PublicQuotationView.revoked("This quotation link has been revoked by the sender.");
        }

        if (q.getTokenExpiresAt() != null && now.isAfter(q.getTokenExpiresAt())) {
            return PublicQuotationView.expired("This quotation link has expired.");
        }

        String businessName = (q.getUser() != null && q.getUser().getName() != null) ? q.getUser().getName() : "MyBill Vendor";
        String clientName = (q.getClient() != null && q.getClient().getName() != null) ? q.getClient().getName() : "Valued Customer";

        return PublicQuotationView.valid(
                rawToken,
                businessName,
                clientName,
                q.getQuotationNumber(),
                q.getTotalAmount() != null ? q.getTotalAmount() : 0.0,
                q.getValidUntilDate(),
                q.getStatus() != null ? q.getStatus().name() : "SENT",
                q.getClientResponseStatus() != null ? q.getClientResponseStatus() : "PENDING",
                q.getRespondedAt(),
                q.getDiscussionMessage()
        );
    }

    @Transactional
    public ResponseSubmissionResult processClientResponse(
            String rawToken, String actionInput, String message, String ipAddress, String userAgent) {

        String tokenHash = hashToken(rawToken);
        if (tokenHash == null) {
            return ResponseSubmissionResult.failure("Invalid link token.");
        }

        Optional<Quotation> optQuote = resolveQuotationByTokenOrId(rawToken, tokenHash);
        if (optQuote.isEmpty()) {
            return ResponseSubmissionResult.failure("Quotation not found.");
        }

        Quotation q = optQuote.get();
        if (Boolean.TRUE.equals(q.getIsDeleted())) {
            return ResponseSubmissionResult.failure("Quotation is no longer available.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (q.getTokenRevokedAt() != null) {
            return ResponseSubmissionResult.failure("This quotation link has been revoked.");
        }

        if (q.getTokenExpiresAt() != null && now.isAfter(q.getTokenExpiresAt())) {
            return ResponseSubmissionResult.failure("This quotation link has expired.");
        }

        String action = actionInput != null ? actionInput.trim().toUpperCase() : "";
        String normalizedAction;
        String clientResponseStatus;
        boolean updateMainStatus = false;
        QuotationStatus newMainStatus = null;

        switch (action) {
            case "ACCEPT":
            case "ACCEPTED":
                normalizedAction = "ACCEPTED";
                clientResponseStatus = "ACCEPTED";
                updateMainStatus = true;
                newMainStatus = QuotationStatus.ACCEPTED;
                break;
            case "DECLINE":
            case "DECLINED":
            case "REJECTED":
                normalizedAction = "DECLINED";
                clientResponseStatus = "DECLINED";
                updateMainStatus = true;
                newMainStatus = QuotationStatus.REJECTED;
                break;
            case "DISCUSS":
            case "DISCUSSION":
            case "DISCUSSION_REQUESTED":
                normalizedAction = "DISCUSSION_REQUESTED";
                clientResponseStatus = "DISCUSSION_REQUESTED";
                updateMainStatus = true;
                newMainStatus = QuotationStatus.DISCUSSION_REQUESTED;
                break;
            default:
                return ResponseSubmissionResult.failure("Unsupported response action: " + actionInput);
        }

        // Idempotency check: if already accepted or declined, prevent duplicate overwrite
        if ("ACCEPTED".equals(q.getClientResponseStatus()) || "DECLINED".equals(q.getClientResponseStatus())) {
            return ResponseSubmissionResult.alreadyResponded(
                    "This quotation has already been " + q.getClientResponseStatus().toLowerCase() + ".",
                    q.getClientResponseStatus()
            );
        }

        // Record updates
        q.setClientResponseStatus(clientResponseStatus);
        q.setRespondedAt(now);
        if (updateMainStatus && newMainStatus != null) {
            q.setStatus(newMainStatus);
        }
        if ("DISCUSSION_REQUESTED".equals(clientResponseStatus) && message != null && !message.isBlank()) {
            q.setDiscussionMessage(message.trim());
        }
        quotationRepository.save(q);

        // Audit log event
        QuotationResponseEvent auditEvent = QuotationResponseEvent.builder()
                .id(UUID.randomUUID())
                .quotation(q)
                .action(normalizedAction)
                .respondedAt(now)
                .discussionMessage(message)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(now)
                .build();
        responseEventRepository.save(auditEvent);

        // Dispatch FCM Notification to owner asynchronously
        String clientName = (q.getClient() != null && q.getClient().getName() != null) ? q.getClient().getName() : "Customer";
        fcmNotificationService.sendQuotationResponseNotification(q, normalizedAction, clientName, message);

        return ResponseSubmissionResult.success(clientResponseStatus, "Thank you! Your response has been recorded.");
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://mybill.onrender.com";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private Optional<Quotation> resolveQuotationByTokenOrId(String rawToken, String tokenHash) {
        Optional<Quotation> opt = quotationRepository.findByPublicTokenHash(tokenHash);
        if (opt.isPresent()) {
            return opt;
        }

        try {
            UUID id = parseUuidOrHex(rawToken);
            if (id != null) {
                Optional<Quotation> found = quotationRepository.findById(id);
                if (found.isPresent()) {
                    Quotation q = found.get();
                    if (q.getPublicTokenHash() == null) {
                        q.setPublicTokenHash(tokenHash);
                        q.setTokenCreatedAt(LocalDateTime.now());
                        q.setTokenExpiresAt(q.getValidUntilDate() != null ? q.getValidUntilDate() : LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
                        quotationRepository.save(q);
                    }
                    return found;
                }
            }
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    private UUID parseUuidOrHex(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        String trimmed = rawToken.trim();
        try {
            return UUID.fromString(trimmed);
        } catch (Exception e) {
            String cleanHex = trimmed.replaceAll("[^a-fA-F0-9]", "");
            if (cleanHex.length() == 32) {
                String uuidStr = cleanHex.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(uuidStr);
            }
        }
        return null;
    }

    public record PublicLinkResult(String token, String fullUrl, LocalDateTime expiresAt) {}

    public record PublicQuotationView(
            boolean isValid,
            boolean isRevoked,
            boolean isExpired,
            String errorMessage,
            String token,
            String businessName,
            String clientName,
            String quotationNumber,
            double totalAmount,
            LocalDateTime validUntilDate,
            String mainStatus,
            String clientResponseStatus,
            LocalDateTime respondedAt,
            String discussionMessage
    ) {
        public static PublicQuotationView invalid(String msg) {
            return new PublicQuotationView(false, false, false, msg, null, null, null, null, 0.0, null, null, null, null, null);
        }
        public static PublicQuotationView revoked(String msg) {
            return new PublicQuotationView(false, true, false, msg, null, null, null, null, 0.0, null, null, null, null, null);
        }
        public static PublicQuotationView expired(String msg) {
            return new PublicQuotationView(false, false, true, msg, null, null, null, null, 0.0, null, null, null, null, null);
        }
        public static PublicQuotationView valid(
                String token, String businessName, String clientName, String quoteNo, double total,
                LocalDateTime validUntil, String mainStatus, String responseStatus, LocalDateTime respondedAt, String discussionMsg) {
            return new PublicQuotationView(true, false, false, null, token, businessName, clientName, quoteNo, total, validUntil, mainStatus, responseStatus, respondedAt, discussionMsg);
        }
    }

    public record ResponseSubmissionResult(
            boolean success,
            boolean alreadyResponded,
            String status,
            String message
    ) {
        public static ResponseSubmissionResult success(String status, String msg) {
            return new ResponseSubmissionResult(true, false, status, msg);
        }
        public static ResponseSubmissionResult alreadyResponded(String msg, String status) {
            return new ResponseSubmissionResult(false, true, status, msg);
        }
        public static ResponseSubmissionResult failure(String msg) {
            return new ResponseSubmissionResult(false, false, null, msg);
        }
    }
}
