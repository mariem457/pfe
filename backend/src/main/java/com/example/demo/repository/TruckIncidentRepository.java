package com.example.demo.repository;

import com.example.demo.entity.Mission;
import com.example.demo.entity.Truck;
import com.example.demo.entity.TruckIncident;
import com.example.demo.entity.TruckIncident.IncidentStatus;
import com.example.demo.entity.TruckIncident.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TruckIncidentRepository extends JpaRepository<TruckIncident, Long> {

    List<TruckIncident> findByTruck(Truck truck);

    List<TruckIncident> findByMission(Mission mission);

    List<TruckIncident> findByStatus(IncidentStatus status);

    List<TruckIncident> findByStatusIn(List<IncidentStatus> statuses);

    List<TruckIncident> findByTruckAndStatus(Truck truck, IncidentStatus status);

    List<TruckIncident> findByTruckAndIncidentType(Truck truck, IncidentType incidentType);

    List<TruckIncident> findByMissionAndStatus(Mission mission, IncidentStatus status);
}