package com.mybill.MyBill_Backend.exception;

/**
 * Thrown when a requested resource does not exist or is not visible to the
 * authenticated user.  Maps to HTTP 404 via {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
