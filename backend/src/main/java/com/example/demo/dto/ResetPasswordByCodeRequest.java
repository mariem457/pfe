package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordByCodeRequest {

    @NotBlank(message = "L'email ou le nom d'utilisateur est obligatoire.")
    private String identifier;

    @NotBlank(message = "Le code est obligatoire.")
    @Pattern(regexp = "^\\d{6}$", message = "Le code doit contenir exactement 6 chiffres.")
    private String code;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial."
    )
    private String newPassword;

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

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}