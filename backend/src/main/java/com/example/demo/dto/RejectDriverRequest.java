package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class RejectDriverRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire.")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}