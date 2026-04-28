package com.example.demo.service;

import com.example.demo.dto.CreateDriverRequest;
import com.example.demo.dto.CreateDriverResponse;
import com.example.demo.dto.DriverBinDto;
import com.example.demo.dto.DriverProfileResponse;
import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.Driver;
import com.example.demo.entity.Mission;
import com.example.demo.entity.MissionBin;
import com.example.demo.entity.Truck;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.MissionBinRepository;
import com.example.demo.repository.MissionRepository;
import com.example.demo.repository.TruckRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final UserRepository userRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final MissionRepository missionRepository;
    private final MissionBinRepository missionBinRepository;
    private final TruckRepository truckRepository;

    public DriverService(
            UserRepository userRepo,
            DriverRepository driverRepo,
            PasswordEncoder passwordEncoder,
            MissionRepository missionRepository,
            MissionBinRepository missionBinRepository,
            TruckRepository truckRepository
    ) {
        this.userRepo = userRepo;
        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
        this.missionRepository = missionRepository;
        this.missionBinRepository = missionBinRepository;
        this.truckRepository = truckRepository;
    }

    @Transactional
    public CreateDriverResponse createDriver(CreateDriverRequest req) {
        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim());
        user.setRole("DRIVER");
        user.setIsEnabled(true);
        user.setMustChangePassword(false);
        user.setAccountStatus(AccountStatus.PENDING);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user = userRepo.save(user);

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFullName(req.getFullName().trim());
        driver.setPhone(req.getPhone().trim());
        driver.setVehicleCode(req.getVehicleCode());
        driver.setIsActive(true);
        driver = driverRepo.save(driver);

        return new CreateDriverResponse(
                user.getId(),
                driver.getId(),
                user.getUsername(),
                user.getRole(),
                user.getAccountStatus().name()
        );
    }

    @Transactional
    public List<DriverBinDto> getMyBins(Long userId) {
        Driver driver = driverRepo.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        List<Mission> missions = missionRepository.findByDriverIdAndPlannedDateOrderByCreatedAtDesc(
                driver.getId(),
                LocalDate.now()
        );

        if (missions.isEmpty()) {
            return Collections.emptyList();
        }

        Mission mission = missions.get(0);

        return missionBinRepository.findByMissionIdOrderByVisitOrderAsc(mission.getId())
                .stream()
                .map(mb -> new DriverBinDto(
                        mission.getId(),
                        mb.getId(),
                        mb.getBin() != null ? mb.getBin().getId() : null,
                        mb.getBin() != null ? mb.getBin().getBinCode() : null,
                        mb.getBin() != null ? mb.getBin().getLat() : null,
                        mb.getBin() != null ? mb.getBin().getLng() : null,
                        mb.getVisitOrder(),
                        mb.isCollected(),
                        mb.getAssignmentStatus() != null ? mb.getAssignmentStatus().name() : null,
                        mb.getBin() != null && mb.getBin().getWasteType() != null
                                ? mb.getBin().getWasteType().name()
                                : null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverProfileResponse getMyProfile(Long userId) {
        Driver driver = driverRepo.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        User user = driver.getUser();

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        List<Mission> monthMissions = missionRepository.findByDriverId(driver.getId())
                .stream()
                .filter(m -> m.getPlannedDate() != null
                        && !m.getPlannedDate().isBefore(firstDayOfMonth)
                        && !m.getPlannedDate().isAfter(today))
                .toList();

        int totalBins = monthMissions.stream()
                .mapToInt(m -> (int) missionBinRepository.countByMissionId(m.getId()))
                .sum();

        int binsCollected = (int) monthMissions.stream()
                .flatMap(m -> missionBinRepository.findByMissionIdOrderByVisitOrderAsc(m.getId()).stream())
                .filter(MissionBin::isCollected)
                .count();

        int routesDone = (int) monthMissions.stream()
                .filter(m -> {
                    List<MissionBin> bins = missionBinRepository.findByMissionIdOrderByVisitOrderAsc(m.getId());
                    return !bins.isEmpty() && bins.stream().allMatch(MissionBin::isCollected);
                })
                .count();

        int efficiency = totalBins > 0
                ? (int) Math.round((binsCollected * 100.0) / totalBins)
                : 0;

        int kmDriven = 0;

        String vehicleCode = driver.getVehicleCode();
        Long assignedTruckId = findTruckIdFromVehicleCode(vehicleCode);

        return new DriverProfileResponse(
                driver.getFullName(),
                user != null ? user.getEmail() : null,
                driver.getPhone(),
                user != null ? user.getUsername() : null,
                "DRV-" + driver.getId(),
                vehicleCode,
                assignedTruckId,
                "Monday - Friday, 8:00 AM - 4:00 PM",
                binsCollected,
                efficiency,
                kmDriven,
                routesDone
        );
    }

    @Transactional
    public DriverProfileResponse getMyProfileByUsernameOrEmail(String usernameOrEmail) {
        User user = userRepo.findByUsername(usernameOrEmail)
                .or(() -> userRepo.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getMyProfile(user.getId());
    }

    private Long findTruckIdFromVehicleCode(String vehicleCode) {
        if (vehicleCode == null || vehicleCode.isBlank()) {
            return null;
        }

        return truckRepository.findAll()
                .stream()
                .filter(t -> vehicleCode.equals(t.getTruckCode()))
                .map(Truck::getId)
                .findFirst()
                .orElse(null);
    }
}