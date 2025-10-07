package com.example.pis.exception;

public class CryptographyException extends RuntimeException {
    public CryptographyException(String message, Throwable cause) {
        super(message, cause);
    }
}
