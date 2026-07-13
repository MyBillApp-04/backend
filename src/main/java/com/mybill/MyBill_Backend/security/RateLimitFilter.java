package com.mybill.MyBill_Backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
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
            "/register"
    );

    private final SecurityUtils securityUtils;

    @Value("${app.security.rate-limit.ip-per-minute:120}")
    private int ipLimitPerMinute;

    @Value("${app.security.rate-limit.user-per-minute:300}")
    private int userLimitPerMinute;

    @Value("${app.security.rate-limit.auth-per-minute:10}")
    private int authLimitPerMinute;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    private final Cache<String, WindowCounter> counters;

    public RateLimitFilter(SecurityUtils securityUtils) {
        this(securityUtils, 5000);
    }

    @Autowired
    public RateLimitFilter(
            SecurityUtils securityUtils,
            @Value("${app.security.rate-limit.cache-max-size:5000}") long cacheMaxSize
    ) {
        this.securityUtils = securityUtils;
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

        String ipKey = "ip:" + clientIp(request);
        if (isExceeded(ipKey, ipLimitPerMinute)) {
            reject(response, "Too many requests from this IP address");
            return;
        }

        if (AUTH_PATHS.contains(path) && isExceeded("auth:" + clientIp(request), authLimitPerMinute)) {
            reject(response, "Too many login attempts. Please wait before trying again");
            return;
        }

        String userKey = currentUserKey();

        // 1. Feature/Endpoint specific rate limiting (Heavy resource protection)
        if (path.contains("/pdf")) {
            String pdfKey = (userKey != null ? userKey : ipKey) + ":pdf";
            if (isExceeded(pdfKey, 10)) { // Capped at 10 requests per minute
                reject(response, "Too many invoice PDF generation requests. Please wait before exporting again");
                return;
            }
        }

        if (path.startsWith("/api/reports")) {
            String reportKey = (userKey != null ? userKey : ipKey) + ":reports";
            if (isExceeded(reportKey, 20)) { // Capped at 20 requests per minute
                reject(response, "Too many reporting & analytics requests. Please wait before reloading reports");
                return;
            }
        }

        // 2. User & Role based rate limiting
        if (userKey != null) {
            int limit = userLimitPerMinute; // Default: 300
            try {
                User user = securityUtils.getCurrentUser();
                if (user != null && user.getRole() == Role.ADMIN) {
                    limit = 5000; // Admins get high throughput limits
                }
            } catch (Exception ignored) {}

            if (isExceeded(userKey, limit)) {
                reject(response, "Too many requests. API rate limit quota exceeded for this account");
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
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstHop = forwardedFor.split(",")[0].trim().toLowerCase(Locale.ROOT);
                if (isValidIpAddress(firstHop)) {
                    return firstHop;
                }
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                String candidate = realIp.trim().toLowerCase(Locale.ROOT);
                if (isValidIpAddress(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
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

    private void reject(HttpServletResponse response, String message) throws IOException {
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
