package com.example.pis.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

public class StripeIntentResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String clientSecret;

    @NotNull
    private String reference;

    public StripeIntentResponseDTO() {}

    public StripeIntentResponseDTO(@NotNull String clientSecret, @NotNull String reference) {
        this.clientSecret = clientSecret;
        this.reference = reference;
    }

    @NotNull
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(@NotNull String clientSecret) { this.clientSecret = clientSecret; }

    @NotNull
    public String getReference() { return reference; }
    public void setReference(@NotNull String reference) { this.reference = reference; }

    @Override
    public String toString() {
        return "StripeIntentResponseDTO{" +
                "clientSecret='" + clientSecret + '\'' +
                ", reference='" + reference + '\'' +
                '}';
    }
}
