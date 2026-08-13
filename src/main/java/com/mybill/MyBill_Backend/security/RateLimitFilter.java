package com.mybill.MyBill_Backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.entity.User;
import io.micrometer.core.instrument.MeterRegistry;
import com.mybill.MyBill_Backend.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/auth/firebase-login",
            "/api/auth/login",
            "/api/auth/register",
            "/auth/login",
            "/auth/register",
            "/api/login",
            "/api/register",
            "/login",
            "/register",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final SecurityUtils securityUtils;
    private final MeterRegistry meterRegistry;

    @Value("${app.security.rate-limit.ip-per-minute:120}")
    private int ipLimitPerMinute;

    @Value("${app.security.rate-limit.user-per-minute:300}")
    private int userLimitPerMinute;

    @Value("${app.security.rate-limit.auth-per-minute:10}")
    private int authLimitPerMinute;

    @Value("${app.security.rate-limit.public-quotation-per-minute:30}")
    private int publicQuotationLimitPerMinute = 30;

    @Value("${app.security.rate-limit.public-quotation-action-per-minute:8}")
    private int publicQuotationActionLimitPerMinute = 8;

    @Value("${app.security.rate-limit.cloudinary-signature-per-minute:5}")
    private int cloudinarySignatureLimitPerMinute = 5;

    @Value("${app.security.rate-limit.cloudinary-metadata-per-minute:10}")
    private int cloudinaryMetadataLimitPerMinute = 10;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Value("${app.security.trusted-proxy-ips:}")
    private String trustedProxyIps;

    private final Cache<String, WindowCounter> counters;

    public RateLimitFilter(SecurityUtils securityUtils) {
        this(securityUtils, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), 5000);
    }

    @Autowired
    public RateLimitFilter(
            SecurityUtils securityUtils,
            MeterRegistry meterRegistry,
            @Value("${app.security.rate-limit.cache-max-size:5000}") long cacheMaxSize
    ) {
        this.securityUtils = securityUtils;
        this.meterRegistry = meterRegistry;
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(2))
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        meterRegistry.counter("security_rate_limit_requests_total", "endpoint", metricEndpoint(path)).increment();

        if (path.startsWith("/q/")) {
            String client = clientIp(request);
            boolean action = "POST".equalsIgnoreCase(request.getMethod()) && path.endsWith("/respond");
            int limit = action ? publicQuotationActionLimitPerMinute : publicQuotationLimitPerMinute;
            String key = "public-quotation:" + (action ? "action:" : "view:") + client;
            if (isExceeded(key, limit)) {
                reject(response, "Too many public quotation requests. Please wait before trying again", action ? "public_quotation_action" : "public_quotation");
                return;
            }
        }

        String ipKey = "ip:" + clientIp(request);
        if (isExceeded(ipKey, ipLimitPerMinute)) {
            reject(response, "Too many requests from this IP address", "ip");
            return;
        }

        if (AUTH_PATHS.contains(path) && isExceeded("auth:" + clientIp(request), authLimitPerMinute)) {
            reject(response, "Too many login attempts. Please wait before trying again", "auth");
            return;
        }

        String userKey = currentUserKey();

        // Financial mutation rate limit
        boolean isFinancialMutate = false;
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            if (path.matches("^/api/clients/[^/]+/financial/payments$") || path.equals("/api/invoice/generate")) {
                isFinancialMutate = true;
            }
        } else if ("PATCH".equalsIgnoreCase(method)) {
            if (path.matches("^/api/invoice/[^/]+/payment$")) {
                isFinancialMutate = true;
            }
        }

        if (isFinancialMutate) {
            String finKey = (userKey != null ? userKey : ipKey) + ":financial_mutate";
            if (isExceeded(finKey, 15)) { // Capped at 15 requests per minute
                reject(response, "Too many financial update requests. Please wait before attempting again", "financial_mutate");
                return;
            }
        }

        // 1. Feature/Endpoint specific rate limiting (Heavy resource protection)
        if (path.contains("/pdf")) {
            String pdfKey = (userKey != null ? userKey : ipKey) + ":pdf";
            if (isExceeded(pdfKey, 10)) { // Capped at 10 requests per minute
                reject(response, "Too many invoice PDF generation requests. Please wait before exporting again", "pdf");
                return;
            }
        }

        if (path.startsWith("/api/reports")) {
            String reportKey = (userKey != null ? userKey : ipKey) + ":reports";
            if (isExceeded(reportKey, 20)) { // Capped at 20 requests per minute
                reject(response, "Too many reporting & analytics requests. Please wait before reloading reports", "reports");
                return;
            }
        }

        if (path.startsWith("/api/business/upload/")) {
            String targetKey = (userKey != null ? userKey : ipKey);
            if (path.endsWith("/signature")) {
                String signatureKey = targetKey + ":cloudinary:signature";
                if (isExceeded(signatureKey, cloudinarySignatureLimitPerMinute)) {
                    reject(response, "Too many Cloudinary upload signature requests. Please wait before requesting another signature", "cloudinary_signature");
                    return;
                }
            } else if (path.endsWith("/metadata")) {
                String metadataKey = targetKey + ":cloudinary:metadata";
                if (isExceeded(metadataKey, cloudinaryMetadataLimitPerMinute)) {
                    reject(response, "Too many Cloudinary image metadata save requests. Please wait before trying again", "cloudinary_metadata");
                    return;
                }
            }
        }

        // 2. User & Role based rate limiting
        if (userKey != null) {
            int limit = userLimitPerMinute; // Default: 300
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                if (isAdmin) {
                    limit = 5000; // Admins get high throughput limits
                }
            }

            if (isExceeded(userKey, limit)) {
                reject(response, "Too many requests. API rate limit quota exceeded for this account", "user");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExceeded(String key, int limit) {
        WindowCounter counter = counters.get(key, ignored -> new WindowCounter());
        return counter.incrementAndCheck(limit);
    }

    private String currentUserKey() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            return userId != null ? "user:" + userId : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders && isTrustedProxy(request.getRemoteAddr())) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                String candidate = realIp.trim().toLowerCase(Locale.ROOT);
                if (isValidIpAddress(candidate)) {
                    return candidate;
                }
            }

            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] hops = forwardedFor.split(",");
                for (int i = hops.length - 1; i >= 0; i--) {
                    String hop = hops[i].trim().toLowerCase(Locale.ROOT);
                    if (isValidIpAddress(hop) && !isTrustedProxy(hop)) {
                        return hop;
                    }
                }
                String lastHop = hops[hops.length - 1].trim().toLowerCase(Locale.ROOT);
                if (isValidIpAddress(lastHop)) {
                    return lastHop;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (!trustForwardedHeaders || trustedProxyIps == null || trustedProxyIps.isBlank() || remoteAddress == null) {
            return false;
        }
        String[] trustedList = trustedProxyIps.split(",");
        for (String trusted : trustedList) {
            String entry = trusted.trim();
            if (entry.isEmpty()) continue;
            if (matchesIpOrCidr(remoteAddress, entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIpOrCidr(String ipStr, String cidrOrIp) {
        if (ipStr.equalsIgnoreCase(cidrOrIp)) {
            return true;
        }
        if (cidrOrIp.contains("/")) {
            try {
                String[] parts = cidrOrIp.split("/");
                InetAddress target = InetAddress.getByName(ipStr);
                InetAddress network = InetAddress.getByName(parts[0]);
                int prefixLength = Integer.parseInt(parts[1]);

                byte[] targetBytes = target.getAddress();
                byte[] networkBytes = network.getAddress();

                if (targetBytes.length != networkBytes.length) {
                    return false;
                }

                int byteCount = prefixLength / 8;
                for (int i = 0; i < byteCount; i++) {
                    if (targetBytes[i] != networkBytes[i]) return false;
                }

                int remBits = prefixLength % 8;
                if (remBits > 0 && byteCount < targetBytes.length) {
                    int mask = (0xFF00 >> remBits) & 0xFF;
                    if ((targetBytes[byteCount] & mask) != (networkBytes[byteCount] & mask)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean isValidIpAddress(String value) {
        if (value == null || value.isBlank() || value.length() > 45) {
            return false;
        }
        if (!value.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String metricEndpoint(String path) {
        if (AUTH_PATHS.contains(path)) return "auth";
        if (path.startsWith("/q/")) return "public_quotation";
        if (path.contains("/pdf")) return "pdf";
        if (path.startsWith("/api/reports")) return "reports";
        if (path.startsWith("/api/business/upload/") && path.endsWith("/signature")) return "cloudinary_signature";
        if (path.startsWith("/api/business/upload/") && path.endsWith("/metadata")) return "cloudinary_metadata";
        return "api";
    }

    private void reject(HttpServletResponse response, String message, String scope) throws IOException {
        meterRegistry.counter("security_rate_limit_rejections_total", "scope", scope).increment();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\""
                + message + "\"}");
    }

    private static class WindowCounter {
        private volatile Instant windowStart = Instant.now();
        private final AtomicInteger count = new AtomicInteger();

        synchronized boolean incrementAndCheck(int limit) {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).toSeconds() >= 60) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() > limit;
        }
    }
}
