package com.example.demo.dto;

public class CreateDriverResponse {

    private Long userId;
    private Long driverId;
    private String username;
    private String role;
    private String accountStatus;

    public CreateDriverResponse() {
    }

    public CreateDriverResponse(Long userId, Long driverId, String username, String role, String accountStatus) {
        this.userId = userId;
        this.driverId = driverId;
        this.username = username;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}