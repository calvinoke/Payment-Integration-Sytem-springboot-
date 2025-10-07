package com.example.pis.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

public class AirtelResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String status; // "SUCCESS", "FAILED"

    @NotNull
    private String body;

    public AirtelResponseDTO() {
        this.status = "";
        this.body = "";
    }

    public AirtelResponseDTO(@NotNull String status, @NotNull String body) {
        this.status = status;
        this.body = body;
    }

    @NotNull
    public String getStatus() { return status; }

    public void setStatus(@NotNull String status) { this.status = status; }

    @NotNull
    public String getBody() { return body; }

    public void setBody(@NotNull String body) { this.body = body; }

    @Override
    public String toString() {
        return "AirtelResponseDTO{" +
                "status='" + status + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
