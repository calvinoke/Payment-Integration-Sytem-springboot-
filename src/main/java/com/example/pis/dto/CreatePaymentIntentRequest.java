package com.example.pis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a Stripe (or other provider) PaymentIntent.
 * <p>
 * Example JSON:
 * {
 *   "amount": 5000,
 *   "currency": "USD"
 * }
 */
public record CreatePaymentIntentRequest(

        /**
         * Amount in the smallest currency unit (e.g. cents).
         * Must be positive.
         */
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be greater than 0")
        Long amount,

        /**
         * Three-letter ISO 4217 currency code (e.g. "USD", "UGX").
         */
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency

) {
    /**
     * Canonical constructor adds an extra runtime guard in case
     * this record is instantiated outside of Bean Validation.
     */
    public CreatePaymentIntentRequest {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || currency.trim().length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
        currency = currency.toUpperCase(); // normalize
    }
}
