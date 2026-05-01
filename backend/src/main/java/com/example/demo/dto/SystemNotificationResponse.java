package com.example.demo.dto;

public class SystemNotificationResponse {

    private String type;
    private String title;
    private String moment;

    public SystemNotificationResponse() {
    }

    public SystemNotificationResponse(String type, String title, String moment) {
        this.type = type;
        this.title = title;
        this.moment = moment;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMoment() { return moment; }

    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMoment(String moment) { this.moment = moment; }
}