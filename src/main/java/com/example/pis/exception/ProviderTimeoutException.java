package com.example.pis.exception;

public class ProviderTimeoutException extends PaymentProcessingException {
    public ProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
