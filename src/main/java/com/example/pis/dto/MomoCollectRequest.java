package com.example.pis.dto;

import jakarta.validation.constraints.*;

/**
 * Request payload for initiating a Mobile Money (MoMo) collection.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "provider": "mtn",
 *   "phone": "256700000000",
 *   "amount": 5000,
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
        @Pattern(
            regexp = "^[0-9]{9,15}$",
            message = "Phone number must contain 9–15 digits"
        )
        String phone,

        /**
         * Amount in the smallest currency unit (e.g. cents or shillings).
         */
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be greater than 0")
        Long amount,

        /**
         * Unique reference to track the transaction in your system.
         */
        @NotBlank(message = "Reference is required")
        @Size(max = 64, message = "Reference cannot exceed 64 characters")
        String reference

) {
    /**
     * Canonical constructor that applies extra runtime checks and
     * normalization in case Bean Validation is bypassed.
     */
    public MomoCollectRequest {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider cannot be blank");
        }

        // Normalize provider to lowercase and trim inputs
        provider = provider.trim().toLowerCase();
        phone = (phone != null) ? phone.trim() : null;
        reference = (reference != null) ? reference.trim() : null;
    }
}
