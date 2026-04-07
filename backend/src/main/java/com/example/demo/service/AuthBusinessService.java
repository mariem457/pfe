package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.DriverRegisterRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordByCodeRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.VerifyResetCodeRequest;
import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.Driver;
import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.sms.SmsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class AuthBusinessService {

    private final UserRepository userRepo;
    private final DriverRepository driverRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public AuthBusinessService(
            UserRepository userRepo,
            DriverRepository driverRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            SmsService smsService
    ) {
        this.userRepo = userRepo;
        this.driverRepository = driverRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void registerDriver(DriverRegisterRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Nom d'utilisateur déjà utilisé.");
        }

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé.");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("DRIVER");
        user.setMustChangePassword(false);
        user.setIsEnabled(true);
        user.setAccountStatus(AccountStatus.PENDING);

        User savedUser = userRepo.save(user);

        Driver driver = new Driver();
        driver.setUser(savedUser);
        driver.setFullName(request.getFullName().trim());
        driver.setPhone(request.getPhone().trim());

        driverRepository.save(driver);
    }

    public void approveDriver(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Cet utilisateur n'est pas un chauffeur.");
        }

        user.setAccountStatus(AccountStatus.APPROVED);
        userRepo.save(user);

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Chauffeur introuvable."));

        if (driver.getPhone() != null && !driver.getPhone().isBlank()) {
            smsService.sendAccountApproved(driver.getPhone());
        }
    }

    public void rejectDriver(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Cet utilisateur n'est pas un chauffeur.");
        }

        user.setAccountStatus(AccountStatus.REJECTED);
        userRepo.save(user);

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Chauffeur introuvable."));

        if (driver.getPhone() != null && !driver.getPhone().isBlank()) {
            smsService.sendAccountRejected(driver.getPhone());
        }
    }

    public RefreshToken createRefreshToken(User user, boolean rememberMe) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(
                rememberMe
                        ? OffsetDateTime.now().plusDays(30)
                        : OffsetDateTime.now().plusDays(1)
        );

        return refreshTokenRepository.save(refreshToken);
    }

    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token invalide"));

        if (refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Refresh token expiré");
        }

        User user = refreshToken.getUser();

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole(),
                user.getTokenVersion()
        );

        return new AuthResponse(
                token,
                user.getRole(),
                user.getId(),
                user.getUsername(),
                user.getMustChangePassword()
        );
    }

    public void forgotPassword(ForgotPasswordRequest request, String frontendUrl) {
        String identifier = request.getUsernameOrEmail() == null
                ? ""
                : request.getUsernameOrEmail().trim();

        User user = userRepo.findByEmail(identifier)
                .or(() -> userRepo.findByUsername(identifier))
                .orElse(null);

        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUser(user);

        if ("DRIVER".equalsIgnoreCase(user.getRole())) {
            Driver driver = driverRepository.findByUser_Id(user.getId()).orElse(null);

            if (driver == null || driver.getPhone() == null || driver.getPhone().isBlank()) {
                return;
            }

            String code = generateVerificationCode();

            PasswordResetToken resetCode = new PasswordResetToken();
            resetCode.setToken(code);
            resetCode.setUser(user);
            resetCode.setUsed(false);
            resetCode.setExpiryDate(OffsetDateTime.now().plusMinutes(10));

            passwordResetTokenRepository.save(resetCode);
            smsService.sendDriverResetCode(driver.getPhone(), code);
            return;
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(OffsetDateTime.now().plusMinutes(30));

        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
    }

    public void verifyResetCode(VerifyResetCodeRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();

        PasswordResetToken resetCode = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getCode())
                .orElseThrow(() -> new RuntimeException("Code invalide"));

        if (resetCode.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Code expiré");
        }

        User user = resetCode.getUser();

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Réinitialisation par code non autorisée pour ce compte.");
        }

        boolean matchesEmail = user.getEmail() != null && user.getEmail().equalsIgnoreCase(identifier);
        boolean matchesUsername = user.getUsername() != null && user.getUsername().equalsIgnoreCase(identifier);

        if (!matchesEmail && !matchesUsername) {
            throw new RuntimeException("Compte incorrect.");
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (resetToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUser(user);
    }

    public void resetPasswordByCode(ResetPasswordByCodeRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();

        PasswordResetToken resetCode = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getCode())
                .orElseThrow(() -> new RuntimeException("Code invalide"));

        if (resetCode.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Code expiré");
        }

        User user = resetCode.getUser();

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Réinitialisation par code non autorisée pour ce compte.");
        }

        boolean matchesEmail = user.getEmail() != null && user.getEmail().equalsIgnoreCase(identifier);
        boolean matchesUsername = user.getUsername() != null && user.getUsername().equalsIgnoreCase(identifier);

        if (!matchesEmail && !matchesUsername) {
            throw new RuntimeException("Compte incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);

        resetCode.setUsed(true);
        passwordResetTokenRepository.save(resetCode);

        refreshTokenRepository.deleteByUser(user);
    }

    private String generateVerificationCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}