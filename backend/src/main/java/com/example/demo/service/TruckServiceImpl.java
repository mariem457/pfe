package com.example.demo.service;

import com.example.demo.dto.TruckRequestDto;
import com.example.demo.dto.TruckResponseDto;
import com.example.demo.dto.TruckStatusUpdateDto;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Truck;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.TruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TruckServiceImpl implements TruckService {

    private final TruckRepository truckRepository;
    private final DriverRepository driverRepository;

    public TruckServiceImpl(TruckRepository truckRepository,
                            DriverRepository driverRepository) {
        this.truckRepository = truckRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public TruckResponseDto createTruck(TruckRequestDto request) {
        if (request.getTruckCode() == null || request.getTruckCode().isBlank()) {
            throw new BadRequestException("Truck code is required");
        }

        truckRepository.findByTruckCode(request.getTruckCode())
                .ifPresent(t -> {
                    throw new ConflictException("Truck code already exists");
                });

        if (request.getPlateNumber() != null && !request.getPlateNumber().isBlank()) {
            truckRepository.findByPlateNumber(request.getPlateNumber())
                    .ifPresent(t -> {
                        throw new ConflictException("Plate number already exists");
                    });
        }

        Truck truck = new Truck();
        mapTruckRequestToEntity(request, truck);

        if (truck.getStatus() == null) {
            truck.setStatus(Truck.TruckStatus.AVAILABLE);
        }

        truck.setLastStatusUpdate(OffsetDateTime.now());

        Truck saved = truckRepository.save(truck);
        return mapToResponse(saved);
    }

    @Override
    public TruckResponseDto updateTruck(Long truckId, TruckRequestDto request) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));

        if (request.getTruckCode() != null && !request.getTruckCode().equals(truck.getTruckCode())) {
            truckRepository.findByTruckCode(request.getTruckCode())
                    .ifPresent(t -> {
                        if (!t.getId().equals(truckId)) {
                            throw new ConflictException("Truck code already exists");
                        }
                    });
        }

        if (request.getPlateNumber() != null && !request.getPlateNumber().isBlank()) {
            truckRepository.findByPlateNumber(request.getPlateNumber())
                    .ifPresent(t -> {
                        if (!t.getId().equals(truckId)) {
                            throw new ConflictException("Plate number already exists");
                        }
                    });
        }

        mapTruckRequestToEntity(request, truck);
        Truck updated = truckRepository.save(truck);
        return mapToResponse(updated);
    }

    @Override
    public TruckResponseDto updateTruckStatus(Long truckId, TruckStatusUpdateDto request) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));

        if (request.getStatus() != null) {
            truck.setStatus(request.getStatus());
        }
        if (request.getFuelLevelLiters() != null) {
            truck.setFuelLevelLiters(BigDecimal.valueOf(request.getFuelLevelLiters()));
        }
        if (request.getCurrentLoadKg() != null) {
            truck.setCurrentLoadKg(BigDecimal.valueOf(request.getCurrentLoadKg()));
        }
        if (request.getLastKnownLat() != null) {
            truck.setLastKnownLat(request.getLastKnownLat());
        }
        if (request.getLastKnownLng() != null) {
            truck.setLastKnownLng(request.getLastKnownLng());
        }

        truck.setLastStatusUpdate(OffsetDateTime.now());

        Truck updated = truckRepository.save(truck);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public TruckResponseDto getTruckById(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));
        return mapToResponse(truck);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckResponseDto> getAllTrucks() {
        return truckRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckResponseDto> getActiveTrucks() {
        return truckRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deactivateTruck(Long truckId) {
        Truck truck = truckRepository.findById(truckId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck not found"));

        truck.setIsActive(false);
        truck.setStatus(Truck.TruckStatus.OUT_OF_SERVICE);
        truck.setLastStatusUpdate(OffsetDateTime.now());

        truckRepository.save(truck);
    }

    private void mapTruckRequestToEntity(TruckRequestDto request, Truck truck) {
        if (request.getTruckCode() != null) {
            truck.setTruckCode(request.getTruckCode());
        }
        if (request.getPlateNumber() != null) {
            truck.setPlateNumber(request.getPlateNumber());
        }
        if (request.getModel() != null) {
            truck.setModel(request.getModel());
        }
        if (request.getBrand() != null) {
            truck.setBrand(request.getBrand());
        }
        if (request.getFuelType() != null) {
            truck.setFuelType(request.getFuelType());
        }
        if (request.getTankCapacityLiters() != null) {
            truck.setTankCapacityLiters(BigDecimal.valueOf(request.getTankCapacityLiters()));
        }
        if (request.getFuelLevelLiters() != null) {
            truck.setFuelLevelLiters(BigDecimal.valueOf(request.getFuelLevelLiters()));
        }
        if (request.getFuelConsumptionPerKm() != null) {
            truck.setFuelConsumptionPerKm(BigDecimal.valueOf(request.getFuelConsumptionPerKm()));
        }
        if (request.getMaxLoadKg() != null) {
            truck.setMaxLoadKg(BigDecimal.valueOf(request.getMaxLoadKg()));
        }
        if (request.getMaxBinCapacity() != null) {
            truck.setMaxBinCapacity(request.getMaxBinCapacity());
        }
        if (request.getCurrentLoadKg() != null) {
            truck.setCurrentLoadKg(BigDecimal.valueOf(request.getCurrentLoadKg()));
        }
        if (request.getStatus() != null) {
            truck.setStatus(request.getStatus());
        }
        if (request.getLastKnownLat() != null) {
            truck.setLastKnownLat(request.getLastKnownLat());
        }
        if (request.getLastKnownLng() != null) {
            truck.setLastKnownLng(request.getLastKnownLng());
        }
        if (request.getIsActive() != null) {
            truck.setIsActive(request.getIsActive());
        }

        if (request.getAssignedDriverId() != null) {
            Driver driver = driverRepository.findById(request.getAssignedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned driver not found"));
            truck.setAssignedDriver(driver);
        }
    }

    private TruckResponseDto mapToResponse(Truck truck) {
        TruckResponseDto dto = new TruckResponseDto();
        dto.setId(truck.getId());
        dto.setTruckCode(truck.getTruckCode());
        dto.setPlateNumber(truck.getPlateNumber());
        dto.setModel(truck.getModel());
        dto.setBrand(truck.getBrand());
        dto.setFuelType(truck.getFuelType());
        dto.setTankCapacityLiters(truck.getTankCapacityLiters());
        dto.setFuelLevelLiters(truck.getFuelLevelLiters());
        dto.setFuelConsumptionPerKm(truck.getFuelConsumptionPerKm());
        dto.setMaxLoadKg(truck.getMaxLoadKg());
        dto.setMaxBinCapacity(truck.getMaxBinCapacity());
        dto.setCurrentLoadKg(truck.getCurrentLoadKg());
        dto.setStatus(truck.getStatus());
        dto.setLastKnownLat(truck.getLastKnownLat());
        dto.setLastKnownLng(truck.getLastKnownLng());
        dto.setLastStatusUpdate(truck.getLastStatusUpdate());
        dto.setIsActive(truck.getIsActive());
        dto.setCreatedAt(truck.getCreatedAt());
        dto.setUpdatedAt(truck.getUpdatedAt());

        if (truck.getAssignedDriver() != null) {
            dto.setAssignedDriverId(truck.getAssignedDriver().getId());
            dto.setAssignedDriverName(truck.getAssignedDriver().getFullName());
        }

        return dto;
    }
}