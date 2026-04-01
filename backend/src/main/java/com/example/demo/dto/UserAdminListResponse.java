package com.example.demo.dto;

import java.time.OffsetDateTime;

public class UserAdminListResponse {
    public Long id;
    public String username;
    public String fullName;
    public String email;
    public String phone;
    public String role;
    public Boolean isEnabled;
    public OffsetDateTime lastLoginAt;
}