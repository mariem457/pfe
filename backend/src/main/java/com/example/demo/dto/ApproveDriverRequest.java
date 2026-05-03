package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class ApproveDriverRequest {

    private Long userId;

    @NotNull(message = "L'identifiant de la demande est obligatoire.")
    private Long requestId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
}