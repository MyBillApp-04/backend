package com.mybill.MyBill_Backend.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlates every HTTP request with application logs and the response.
 *
 * <p>A caller-supplied {@value #REQUEST_ID_HEADER} is retained when it contains
 * only safe correlation characters; otherwise a UUID is generated. The value is
 * available to downstream code through SLF4J MDC under {@code requestId}.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    /** HTTP request and response header carrying the correlation identifier. */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /** SLF4J MDC key emitted by the structured logging configuration. */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        long startedAt = System.nanoTime();

        try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID_MDC_KEY, requestId)) {
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID_MDC_KEY, requestId)) {
                log.info("http_request method={} path={} status={} duration_ms={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), durationMs);
            }
        }
    }

    /**
     * Returns a safe client correlation ID or generates a new UUID.
     *
     * @param candidate value supplied in {@value #REQUEST_ID_HEADER}
     * @return validated correlation identifier
     */
    public String resolveRequestId(String candidate) {
        return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }
}
