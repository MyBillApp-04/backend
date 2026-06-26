package com.mybill.MyBill_Backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/auth/firebase-login"
    );

    private final SecurityUtils securityUtils;

    @Value("${app.security.rate-limit.ip-per-minute:120}")
    private int ipLimitPerMinute;

    @Value("${app.security.rate-limit.user-per-minute:300}")
    private int userLimitPerMinute;

    @Value("${app.security.rate-limit.auth-per-minute:10}")
    private int authLimitPerMinute;

    private final Cache<String, WindowCounter> counters = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(50_000)
            .build();

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
        if (userKey != null && isExceeded(userKey, userLimitPerMinute)) {
            reject(response, "Too many requests for this user");
            return;
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim().toLowerCase(Locale.ROOT);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim().toLowerCase(Locale.ROOT);
        }
        return request.getRemoteAddr();
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
