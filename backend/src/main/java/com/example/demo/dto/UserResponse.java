package com.example.demo.dto;

import java.time.OffsetDateTime;

public class UserResponse {
    public Long id;
    public String username;
    public String email;
    public String role;
    public Boolean isEnabled;
    public OffsetDateTime lastLoginAt;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
