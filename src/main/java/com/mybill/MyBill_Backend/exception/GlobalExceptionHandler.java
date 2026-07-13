package com.mybill.MyBill_Backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import com.mybill.MyBill_Backend.observability.RequestCorrelationFilter;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    // NotFoundException, ForbiddenException, ConflictException, and FileStorageException
    // are defined as top-level classes in the exception package.

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Not found: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorage(
            FileStorageException ex,
            HttpServletRequest request
    ) {
        log.error("File storage error: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "File could not be retrieved. Please try again later",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Invalid value"
                )
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Validation failed");
        body.put("path", request.getRequestURI());
        body.put("requestId", currentRequestId());
        body.put("fieldErrors", fieldErrors);

        incrementErrorMetric(HttpStatus.BAD_REQUEST, ex, request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, Object>> handleUploadException(
            UploadException ex,
            HttpServletRequest request
    ) {
        log.warn("Image upload failed: code={} exception={}", ex.getCode(), ex.getClass().getSimpleName());
        ResponseEntity<Map<String, Object>> response = buildErrorResponse(
                ex.getStatus(), ex.getMessage(), request.getRequestURI(), ex);
        response.getBody().put("code", ex.getCode());
        return response;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "Image must be 10 MB or smaller.", request.getRequestURI(), ex);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingUploadFile(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Image file is required. Please choose an image and try again.", request.getRequestURI(), ex);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedUpload(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed image upload: exception={}", ex.getClass().getSimpleName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Image upload format is invalid. Please choose the image again and retry.",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed request body for {}: exception={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Request body is invalid. Please refresh and try again.",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required request header: " + ex.getHeaderName(),
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("API endpoint not found: {}", request.getRequestURI());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "API endpoint not found",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request. Please check the submitted data",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(
            SecurityException ex,
            HttpServletRequest request
    ) {
        log.warn("Security violation: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Request failed security validation",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(),
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Database error: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database error. Please try again later",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Database constraint violation for {}: exception={}",
                request.getRequestURI(), ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "A conflicting record already exists. Refresh and try again",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(
            TransactionSystemException ex,
            HttpServletRequest request
    ) {
        log.error("Database transaction commit failed: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database save failed. Please check the submitted profile details",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected runtime exception: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later",
                request.getRequestURI(),
                ex
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected exception: exception={}", ex.getClass().getSimpleName());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error. Please try again later",
                request.getRequestURI(),
                ex
        );
    }

    private void incrementErrorMetric(HttpStatus status, Throwable ex, String path) {
        if (meterRegistry != null && status != null && status.isError()) {
            try {
                String exName = ex != null ? ex.getClass().getSimpleName() : "None";
                meterRegistry.counter("mybill.api.errors",
                        "status", String.valueOf(status.value()),
                        "exception", exName,
                        "path", path != null ? path : "unknown"
                ).increment();
            } catch (Exception e) {
                // Ignore metric execution errors
            }
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Throwable ex
    ) {
        incrementErrorMetric(status, ex, path);

        jakarta.servlet.http.HttpServletRequest request = null;
        try {
            org.springframework.web.context.request.RequestAttributes attributes = 
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
                request = servletAttributes.getRequest();
            }
        } catch (Exception e) {
            // Ignore if request context is not active
        }

        String userIdStr = "ANONYMOUS";
        String invoiceIdStr = "NONE";
        if (request != null) {
            try {
                org.springframework.security.core.Authentication auth = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    if (auth.getDetails() instanceof Long userId) {
                        userIdStr = String.valueOf(userId);
                    } else {
                        userIdStr = auth.getName();
                    }
                }
            } catch (Exception e) {
                // Ignore security context failures
            }

            try {
                String uri = request.getRequestURI();
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                        "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
                ).matcher(uri);
                if (matcher.find()) {
                    invoiceIdStr = matcher.group();
                } else {
                    String paramVal = request.getParameter("invoiceId");
                    if (paramVal != null && !paramVal.isBlank()) {
                        invoiceIdStr = paramVal;
                    }
                }
            } catch (Exception e) {
                // Ignore parsing failures
            }

        }

        logException(status, path, userIdStr, invoiceIdStr, message, ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        body.put("requestId", currentRequestId());

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private void logException(
            HttpStatus status,
            String path,
            String userId,
            String resourceId,
            String message,
            Throwable ex
    ) {
        if (status.is5xxServerError()) {
            log.error("api_error status={} path={} userId={} resourceId={} exception={} message={} requestId={}",
                    status.value(), path, userId, resourceId,
                    ex != null ? ex.getClass().getSimpleName() : "None",
                    SecureLogMessageConverter.sanitize(message),
                    currentRequestId());
            return;
        }

        log.warn("api_error status={} path={} userId={} resourceId={} exception={} message={}",
                status.value(), path, userId, resourceId,
                ex != null ? ex.getClass().getSimpleName() : "None",
                SecureLogMessageConverter.sanitize(message));
    }

    private String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "n/a";
        }
        return SecureLogMessageConverter.sanitize(ex.getMessage());
    }

    private String currentRequestId() {
        String requestId = MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY);
        return requestId != null && !requestId.isBlank() ? requestId : "unknown";
    }
}
