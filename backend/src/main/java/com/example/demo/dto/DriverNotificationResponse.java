package com.example.demo.dto;

import com.example.demo.entity.DriverNotification;
import java.time.OffsetDateTime;

public class DriverNotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String message;
    private OffsetDateTime createdAt;
    private Boolean read;

    public DriverNotificationResponse(DriverNotification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.createdAt = notification.getCreatedAt();
        this.read = notification.getRead();
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Boolean getRead() { return read; }
}