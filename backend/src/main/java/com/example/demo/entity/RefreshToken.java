package com.example.demo.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 500)
    private String token;

    @ManyToOne(optional = false)
    private User user;

    private OffsetDateTime expiryDate;

    private boolean revoked = false;

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public OffsetDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(OffsetDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}