package com.example.demo.dto;

public class SystemOverviewResponse {

    private int cpuUsage;
    private double memoryUsedGb;
    private double memoryTotalGb;
    private int activeServices;
    private int totalServices;
    private String uptime;

    public SystemOverviewResponse() {
    }

    public SystemOverviewResponse(int cpuUsage, double memoryUsedGb, double memoryTotalGb,
                                  int activeServices, int totalServices, String uptime) {
        this.cpuUsage = cpuUsage;
        this.memoryUsedGb = memoryUsedGb;
        this.memoryTotalGb = memoryTotalGb;
        this.activeServices = activeServices;
        this.totalServices = totalServices;
        this.uptime = uptime;
    }

    public int getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(int cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getMemoryUsedGb() { return memoryUsedGb; }
    public void setMemoryUsedGb(double memoryUsedGb) { this.memoryUsedGb = memoryUsedGb; }

    public double getMemoryTotalGb() { return memoryTotalGb; }
    public void setMemoryTotalGb(double memoryTotalGb) { this.memoryTotalGb = memoryTotalGb; }

    public int getActiveServices() { return activeServices; }
    public void setActiveServices(int activeServices) { this.activeServices = activeServices; }

    public int getTotalServices() { return totalServices; }
    public void setTotalServices(int totalServices) { this.totalServices = totalServices; }

    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }
}