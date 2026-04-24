package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class ApproveDriverRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire.")
    private Long requestId;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}