package com.example.demo.dto;

public class SystemDatabaseStatusResponse {

    private String activeConnections;
    private String databaseSize;
    private String queriesPerSecond;
    private String lastBackup;

    public SystemDatabaseStatusResponse() {
    }

    public SystemDatabaseStatusResponse(String activeConnections, String databaseSize,
                                        String queriesPerSecond, String lastBackup) {
        this.activeConnections = activeConnections;
        this.databaseSize = databaseSize;
        this.queriesPerSecond = queriesPerSecond;
        this.lastBackup = lastBackup;
    }

    public String getActiveConnections() { return activeConnections; }
    public void setActiveConnections(String activeConnections) { this.activeConnections = activeConnections; }

    public String getDatabaseSize() { return databaseSize; }
    public void setDatabaseSize(String databaseSize) { this.databaseSize = databaseSize; }

    public String getQueriesPerSecond() { return queriesPerSecond; }
    public void setQueriesPerSecond(String queriesPerSecond) { this.queriesPerSecond = queriesPerSecond; }

    public String getLastBackup() { return lastBackup; }
    public void setLastBackup(String lastBackup) { this.lastBackup = lastBackup; }
}