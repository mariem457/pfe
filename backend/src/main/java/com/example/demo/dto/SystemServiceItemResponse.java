package com.example.demo.dto;

public class SystemServiceItemResponse {

    private String name;
    private String description;
    private String status;
    private String uptime;
    private int cpuUsage;
    private int memoryUsage;

    public SystemServiceItemResponse() {
    }

    public SystemServiceItemResponse(String name, String description, String status,
                                     String uptime, int cpuUsage, int memoryUsage) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.uptime = uptime;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getUptime() {
        return uptime;
    }

    public int getCpuUsage() {
        return cpuUsage;
    }

    public int getMemoryUsage() {
        return memoryUsage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public void setCpuUsage(int cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public void setMemoryUsage(int memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
}