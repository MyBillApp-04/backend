package com.mybill.MyBill_Backend.exception;

/**
 * Thrown when the authenticated user does not have permission to access a
 * specific resource.  Maps to HTTP 403 via {@link GlobalExceptionHandler}.
 *
 * <p>Note: Spring Security's {@code AccessDeniedException} is also mapped to 403
 * by the handler; this class is for application-level access checks (e.g.,
 * verifying a file belongs to the requesting user).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
