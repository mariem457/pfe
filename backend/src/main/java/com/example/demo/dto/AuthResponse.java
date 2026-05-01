package com.example.demo.dto;

public class AuthResponse {
    private String token;
    private String role;
    private Long userId;
    private String username;
    private Boolean mustChangePassword;

    public AuthResponse() {}

    public AuthResponse(String token, String role, Long userId, String username,Boolean mustChangePassword) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.username = username;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() { return token; }
    public Boolean getMustChangePassword() {
		return mustChangePassword;
	}

	public void setMustChangePassword(Boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}

	public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}