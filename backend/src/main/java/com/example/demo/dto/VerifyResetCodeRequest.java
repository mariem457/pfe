package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyResetCodeRequest {

    @NotBlank(message = "L'email ou le nom d'utilisateur est obligatoire.")
    private String identifier;

    @NotBlank(message = "Le code est obligatoire.")
    @Pattern(regexp = "^\\d{6}$", message = "Le code doit contenir exactement 6 chiffres.")
    private String code;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}