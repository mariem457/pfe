package com.example.demo.dto;

public class DriverProfileResponse {
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String driverId;
    private String assignedTruck;
    private Long assignedTruckId;
    private String shiftSchedule;
    private Integer binsCollected;
    private Integer efficiency;
    private Integer kmDriven;
    private Integer routesDone;

    public DriverProfileResponse() {
    }

    public DriverProfileResponse(
            String fullName,
            String email,
            String phone,
            String username,
            String driverId,
            String assignedTruck,
            Long assignedTruckId,
            String shiftSchedule,
            Integer binsCollected,
            Integer efficiency,
            Integer kmDriven,
            Integer routesDone
    ) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.driverId = driverId;
        this.assignedTruck = assignedTruck;
        this.assignedTruckId = assignedTruckId;
        this.shiftSchedule = shiftSchedule;
        this.binsCollected = binsCollected;
        this.efficiency = efficiency;
        this.kmDriven = kmDriven;
        this.routesDone = routesDone;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getAssignedTruck() { return assignedTruck; }
    public void setAssignedTruck(String assignedTruck) { this.assignedTruck = assignedTruck; }

    public Long getAssignedTruckId() { return assignedTruckId; }
    public void setAssignedTruckId(Long assignedTruckId) { this.assignedTruckId = assignedTruckId; }

    public String getShiftSchedule() { return shiftSchedule; }
    public void setShiftSchedule(String shiftSchedule) { this.shiftSchedule = shiftSchedule; }

    public Integer getBinsCollected() { return binsCollected; }
    public void setBinsCollected(Integer binsCollected) { this.binsCollected = binsCollected; }

    public Integer getEfficiency() { return efficiency; }
    public void setEfficiency(Integer efficiency) { this.efficiency = efficiency; }

    public Integer getKmDriven() { return kmDriven; }
    public void setKmDriven(Integer kmDriven) { this.kmDriven = kmDriven; }

    public Integer getRoutesDone() { return routesDone; }
    public void setRoutesDone(Integer routesDone) { this.routesDone = routesDone; }
}