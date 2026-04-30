package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    private String email;

    private String usernameOrEmail;

    public ForgotPasswordRequest() {
    }

    public String getEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return usernameOrEmail;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsernameOrEmail() {
        if (usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            return usernameOrEmail;
        }
        return email;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}