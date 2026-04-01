package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "security_settings")
public class SecuritySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = true;

    @Column(name = "login_notifications", nullable = false)
    private Boolean loginNotifications = true;

    @Column(name = "api_rate_limiting", nullable = false)
    private Boolean apiRateLimiting = true;

    @Column(name = "suspicious_activity_detection", nullable = false)
    private Boolean suspiciousActivityDetection = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (twoFactorEnabled == null) twoFactorEnabled = true;
        if (loginNotifications == null) loginNotifications = true;
        if (apiRateLimiting == null) apiRateLimiting = true;
        if (suspiciousActivityDetection == null) suspiciousActivityDetection = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public Boolean getLoginNotifications() { return loginNotifications; }
    public void setLoginNotifications(Boolean loginNotifications) { this.loginNotifications = loginNotifications; }

    public Boolean getApiRateLimiting() { return apiRateLimiting; }
    public void setApiRateLimiting(Boolean apiRateLimiting) { this.apiRateLimiting = apiRateLimiting; }

    public Boolean getSuspiciousActivityDetection() { return suspiciousActivityDetection; }
    public void setSuspiciousActivityDetection(Boolean suspiciousActivityDetection) { this.suspiciousActivityDetection = suspiciousActivityDetection; }
}