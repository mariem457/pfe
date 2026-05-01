package com.example.demo.service;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.DriverRegistrationRequestEntity;
import com.example.demo.repository.DriverRegistrationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmailVerificationService {

    private final DriverRegistrationRequestRepository driverRegistrationRequestRepository;
    private final EmailService emailService;

    public EmailVerificationService(
            DriverRegistrationRequestRepository driverRegistrationRequestRepository,
            EmailService emailService
    ) {
        this.driverRegistrationRequestRepository = driverRegistrationRequestRepository;
        this.emailService = emailService;
    }

    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    @Transactional
    public void sendVerificationCode(String email) {
        DriverRegistrationRequestEntity request = driverRegistrationRequestRepository
                .findByEmailAndStatus(email, AccountStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Demande d'inscription introuvable."));

        String code = generateCode();

        request.setEmailVerificationCode(code);
        request.setEmailVerificationExpiry(OffsetDateTime.now().plusMinutes(10));
        driverRegistrationRequestRepository.save(request);

        emailService.sendVerificationEmail(request.getEmail(), code);
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        DriverRegistrationRequestEntity request = driverRegistrationRequestRepository
                .findByEmailAndStatus(email, AccountStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Demande d'inscription introuvable."));

        if (Boolean.TRUE.equals(request.getEmailVerified())) {
            return;
        }

        if (request.getEmailVerificationCode() == null || request.getEmailVerificationExpiry() == null) {
            throw new RuntimeException("Aucun code de vérification trouvé.");
        }

        if (OffsetDateTime.now().isAfter(request.getEmailVerificationExpiry())) {
            throw new RuntimeException("Le code a expiré.");
        }

        if (!request.getEmailVerificationCode().equals(code)) {
            throw new RuntimeException("Code invalide.");
        }

        request.setEmailVerified(true);
        request.setEmailVerificationCode(null);
        request.setEmailVerificationExpiry(null);

        driverRegistrationRequestRepository.save(request);
    }
}