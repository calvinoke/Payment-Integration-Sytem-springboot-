package com.example.pis.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

public class StripePayoutResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String payoutId;

    @NotNull
    private String reference;

    public StripePayoutResponseDTO() {}

    public StripePayoutResponseDTO(@NotNull String payoutId, @NotNull String reference) {
        this.payoutId = payoutId;
        this.reference = reference;
    }

    @NotNull
    public String getPayoutId() { return payoutId; }
    public void setPayoutId(@NotNull String payoutId) { this.payoutId = payoutId; }

    @NotNull
    public String getReference() { return reference; }
    public void setReference(@NotNull String reference) { this.reference = reference; }

    @Override
    public String toString() {
        return "StripePayoutResponseDTO{" +
                "payoutId='" + payoutId + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
