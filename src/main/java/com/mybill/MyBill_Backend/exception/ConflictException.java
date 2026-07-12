package com.mybill.MyBill_Backend.exception;

/**
 * Thrown when a request cannot be completed because it conflicts with the
 * current state of a resource (e.g., duplicate invoice number, already billed
 * work).  Maps to HTTP 409 via {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
