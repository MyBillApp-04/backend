package com.mybill.MyBill_Backend.exception;

/**
 * Thrown when a file cannot be read from or written to the storage layer.
 * Maps to HTTP 500 via {@link GlobalExceptionHandler}.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
