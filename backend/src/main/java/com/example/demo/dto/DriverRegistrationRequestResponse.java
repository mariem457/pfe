package com.example.demo.dto;

import java.time.OffsetDateTime;

public class DriverRegistrationRequestResponse {
    public Long id;
    public String fullName;
    public String username;
    public String email;
    public String phone;
    public String status;
    public OffsetDateTime createdAt;
    public Boolean emailVerified;
}