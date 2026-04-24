package com.example.demo.repository;

import com.example.demo.entity.DriverRegistrationRequestEntity;
import com.example.demo.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRegistrationRequestRepository
        extends JpaRepository<DriverRegistrationRequestEntity, Long> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<DriverRegistrationRequestEntity> findByStatus(AccountStatus status);

    Optional<DriverRegistrationRequestEntity> findByEmail(String email);

    Optional<DriverRegistrationRequestEntity> findByEmailAndStatus(
            String email,
            AccountStatus status
    );
}