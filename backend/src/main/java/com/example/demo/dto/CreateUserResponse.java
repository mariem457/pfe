package com.example.demo.dto;

public class CreateUserResponse {
    private Long userId;
    private String username;
    private String role;
    private String temporaryPassword;

    public CreateUserResponse() {}

    public CreateUserResponse(Long userId, String username, String role, String temporaryPassword) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getTemporaryPassword() { return temporaryPassword; }
}
