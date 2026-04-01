package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.SystemSettings;
import com.example.demo.repository.SystemSettingsRepository;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.sun.management.OperatingSystemMXBean;

@Service
public class SystemControlService {

    private final DataSource dataSource;
    private final SystemSettingsRepository systemSettingsRepository;

    public SystemControlService(DataSource dataSource, SystemSettingsRepository systemSettingsRepository) {
        this.dataSource = dataSource;
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public SystemOverviewResponse getOverview() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuLoad = osBean.getSystemCpuLoad();
        if (cpuLoad < 0) cpuLoad = 0;
        int cpuUsage = (int) Math.round(cpuLoad * 100);

        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();
        long usedMemory = totalMemory - freeMemory;

        double totalGb = totalMemory / 1_000_000_000.0;
        double usedGb = usedMemory / 1_000_000_000.0;

        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);

        long days = uptime.toDays();
        long hours = uptime.toHoursPart();

        List<SystemComponentResponse> components = getComponents();

        int activeServices = (int) components.stream()
                .filter(c -> "RUNNING".equalsIgnoreCase(c.getStatus()))
                .count();

        return new SystemOverviewResponse(
                cpuUsage,
                round(usedGb),
                round(totalGb),
                activeServices,
                components.size(),
                days + " j " + hours + " h"
        );
    }

    public List<SystemComponentResponse> getComponents() {
        List<SystemComponentResponse> list = new ArrayList<>();

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        boolean dbUp = checkDatabase();
        String uptime = getUptime();

        double jvmHeapUsedMb = heap.getUsed() / 1_000_000.0;
        double jvmHeapMaxMb = heap.getMax() / 1_000_000.0;

        list.add(new SystemComponentResponse(
                "Spring Boot API",
                "Application backend principale",
                "RUNNING",
                "Uptime", uptime,
                "Threads", String.valueOf(threadMXBean.getThreadCount()),
                "Heap JVM", round(jvmHeapUsedMb) + " / " + round(jvmHeapMaxMb) + " MB"
        ));

        list.add(new SystemComponentResponse(
                "PostgreSQL",
                "Base de données principale",
                dbUp ? "RUNNING" : "STOPPED",
                "Connexions", getDbConnectionsReal(),
                "Taille DB", getDbSizeReal(),
                "Version", getDbVersionReal()
        ));

        File root = new File("/");

        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;

        double totalDiskGb = totalDisk / 1_000_000_000.0;
        double freeDiskGb = freeDisk / 1_000_000_000.0;
        double usedDiskPercent = totalDisk > 0 ? ((double) usedDisk / totalDisk) * 100.0 : 0.0;

        list.add(new SystemComponentResponse(
                "Disque système",
                "Stockage principal du serveur",
                freeDiskGb > 2 ? "RUNNING" : "STOPPED",
                "Utilisé", round(usedDiskPercent) + " %",
                "Libre", round(freeDiskGb) + " Go",
                "Total", round(totalDiskGb) + " Go"
        ));

        int cpuUsage = (int) Math.round(Math.max(0, osBean.getSystemCpuLoad()) * 100);

        list.add(new SystemComponentResponse(
                "Hôte système",
                "Ressources système du serveur",
                "RUNNING",
                "CPU", cpuUsage + " %",
                "Cœurs", String.valueOf(Runtime.getRuntime().availableProcessors()),
                "Architecture", osBean.getArch()
        ));

        double totalRamGb = osBean.getTotalPhysicalMemorySize() / 1_000_000_000.0;
        double freeRamGb = osBean.getFreePhysicalMemorySize() / 1_000_000_000.0;
        double usedRamGb = totalRamGb - freeRamGb;

        list.add(new SystemComponentResponse(
                "Mémoire physique",
                "Utilisation réelle de la RAM",
                "RUNNING",
                "Utilisée", round(usedRamGb) + " Go",
                "Libre", round(freeRamGb) + " Go",
                "Totale", round(totalRamGb) + " Go"
        ));

        return list;
    }

    public List<SystemNotificationResponse> getNotifications() {
        List<SystemNotificationResponse> list = new ArrayList<>();

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuLoad = Math.max(0, osBean.getSystemCpuLoad());
        int cpu = (int) Math.round(cpuLoad * 100);

        if (checkDatabase()) {
            list.add(new SystemNotificationResponse(
                    "SUCCESS",
                    "Base de données opérationnelle",
                    "Maintenant"
            ));
        } else {
            list.add(new SystemNotificationResponse(
                    "ALERT",
                    "Base de données indisponible",
                    "Maintenant"
            ));
        }

        if (cpu > 80) {
            list.add(new SystemNotificationResponse(
                    "ALERT",
                    "Utilisation CPU élevée",
                    "Maintenant"
            ));
        }

        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        double freePercent = totalDisk > 0 ? ((double) freeDisk / totalDisk) * 100.0 : 0.0;

        if (freePercent < 10) {
            list.add(new SystemNotificationResponse(
                    "ALERT",
                    "Espace disque faible",
                    "Maintenant"
            ));
        }

        list.add(new SystemNotificationResponse(
                "INFO",
                "Système opérationnel",
                "Maintenant"
        ));

        return list;
    }

    public SystemDatabaseStatusResponse getDatabaseStatus() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet sizeRs = stmt.executeQuery("SELECT pg_database_size(current_database())");
            long dbSize = 0;
            if (sizeRs.next()) {
                dbSize = sizeRs.getLong(1);
            }

            ResultSet connRs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity");
            int activeConnections = 0;
            if (connRs.next()) {
                activeConnections = connRs.getInt(1);
            }

            ResultSet statRs = stmt.executeQuery(
                    "SELECT sum(xact_commit + xact_rollback) FROM pg_stat_database"
            );
            long queries = 0;
            if (statRs.next()) {
                queries = statRs.getLong(1);
            }

            return new SystemDatabaseStatusResponse(
                    String.valueOf(activeConnections),
                    round(dbSize / 1_000_000_000.0) + " Go",
                    String.valueOf(queries),
                    "Base active"
            );

        } catch (Exception e) {
            return new SystemDatabaseStatusResponse(
                    "Erreur",
                    "0 Go",
                    "0",
                    "DB inaccessible"
            );
        }
    }

    public SystemSettingsResponse getSettings() {
        SystemSettings settings = getOrCreateSettings();
        return new SystemSettingsResponse(
                Boolean.TRUE.equals(settings.getMaintenanceMode()),
                Boolean.TRUE.equals(settings.getAutomaticBackup()),
                Boolean.TRUE.equals(settings.getRealtimeMonitoring())
        );
    }

    public SystemSettingsResponse updateSettings(UpdateSystemSettingsRequest req) {
        SystemSettings settings = getOrCreateSettings();

        if (req.getMaintenanceMode() != null) {
            settings.setMaintenanceMode(req.getMaintenanceMode());
        }

        if (req.getAutomaticBackup() != null) {
            settings.setAutomaticBackup(req.getAutomaticBackup());
        }

        if (req.getRealtimeMonitoring() != null) {
            settings.setRealtimeMonitoring(req.getRealtimeMonitoring());
        }

        settings = systemSettingsRepository.save(settings);

        return new SystemSettingsResponse(
                Boolean.TRUE.equals(settings.getMaintenanceMode()),
                Boolean.TRUE.equals(settings.getAutomaticBackup()),
                Boolean.TRUE.equals(settings.getRealtimeMonitoring())
        );
    }

    private SystemSettings getOrCreateSettings() {
        return systemSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
            SystemSettings settings = new SystemSettings();
            settings.setMaintenanceMode(false);
            settings.setAutomaticBackup(true);
            settings.setRealtimeMonitoring(true);
            return systemSettingsRepository.save(settings);
        });
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private String getUptime() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);
        return uptime.toDays() + " j " + uptime.toHoursPart() + " h";
    }

    private String getDbConnectionsReal() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity")) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (Exception ignored) {
        }
        return "N/A";
    }

    private String getDbSizeReal() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT pg_database_size(current_database())")) {
            if (rs.next()) {
                long dbSize = rs.getLong(1);
                return round(dbSize / 1_000_000_000.0) + " Go";
            }
        } catch (Exception ignored) {
        }
        return "N/A";
    }

    private String getDbVersionReal() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            if (rs.next()) {
                String version = rs.getString(1);
                return version.length() > 30 ? version.substring(0, 30) + "..." : version;
            }
        } catch (Exception ignored) {
        }
        return "N/A";
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}