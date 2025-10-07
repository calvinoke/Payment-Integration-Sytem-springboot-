package com.example.pis.dto;

import com.example.pis.enums.SupportedCurrency;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for initiating a Mobile Money (MoMo) collection.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "provider": "mtn",
 *   "phone": "256700000000",
 *   "amount": 5000,
 *   "currency": "UGX",
 *   "reference": "INV-2025-001"
 * }
 * }</pre>
 */
public record MomoCollectRequest(

        /**
         * Mobile money provider identifier, e.g. "mtn" or "airtel".
         */
        @NotBlank(message = "Provider is required")
        @Size(min = 2, max = 20, message = "Provider must be 2–20 characters")
        String provider,

        /**
         * MSISDN (phone number) in international format without the plus sign.
         * Example: 256700000000
         */
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must contain 9–15 digits")
        String phone,

        /**
         * Amount in the smallest currency unit (e.g., cents or shillings).
         */
        @Min(value = 1, message = "Amount must be greater than 0")
        Long amount,

        /**
         * Currency code, e.g., USD, UGX, KES.
         */
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 letters")
        String currency,

        /**
         * Unique reference to track the transaction in your system.
         */
        @NotBlank(message = "Reference is required")
        @Size(max = 64, message = "Reference cannot exceed 64 characters")
        String reference

) {
    /**
     * Canonical constructor that applies runtime checks and normalization.
     */
    public MomoCollectRequest {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (provider.isBlank()) {
            throw new IllegalArgumentException("Provider cannot be blank");
        }

        // Normalize inputs
        provider = provider.trim().toLowerCase();
        phone = phone.trim();
        reference = reference.trim();
        currency = currency.trim().toUpperCase();

        // Validate supported currency
        if (!SupportedCurrency.CODES.contains(currency)) {
            throw new IllegalArgumentException("Currency '" + currency + "' is not supported");
        }
    }
}
