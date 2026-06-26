package com.mybill.MyBill_Backend.exception;

import org.springframework.http.HttpStatus;

/** A user-safe, structured error produced while accepting a business image. */
public class UploadException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public UploadException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public UploadException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
