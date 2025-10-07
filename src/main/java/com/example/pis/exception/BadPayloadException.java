package com.example.pis.exception;

public class BadPayloadException extends RuntimeException {
    public BadPayloadException(String message) {
        super(message);
    }

    public BadPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
