package com.example.demo.dto;

public class CreateDriverResponse {
    private Long userId;
    private Long driverId;
    private String username;
    private String role;
    private String temporaryPassword;

    public CreateDriverResponse() {}

    public CreateDriverResponse(Long userId, Long driverId, String username, String role, String temporaryPassword) {
        this.userId = userId;
        this.driverId = driverId;
        this.username = username;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
    }

    public Long getUserId() { return userId; }
    public Long getDriverId() { return driverId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getTemporaryPassword() { return temporaryPassword; }
}