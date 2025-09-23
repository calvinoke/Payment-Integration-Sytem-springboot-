package com.example.pis.dto;

import java.io.Serializable;

public class MtnResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status; // "SUCCESS", "FAILED"
    private String body;

    public MtnResponseDTO() {}

    public MtnResponseDTO(String status, String body) {
        this.status = status;
        this.body = body;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    @Override
    public String toString() {
        return "MtnResponseDTO{" +
                "status='" + status + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
