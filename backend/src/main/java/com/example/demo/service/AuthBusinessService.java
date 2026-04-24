package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.DriverRegisterRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordByCodeRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.VerifyResetCodeRequest;
import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.Driver;
import com.example.demo.entity.DriverRegistrationRequestEntity;
import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.repository.DriverRegistrationRequestRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class AuthBusinessService {

    private final UserRepository userRepo;
    private final DriverRepository driverRepository;
    private final DriverRegistrationRequestRepository driverRegistrationRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public AuthBusinessService(
            UserRepository userRepo,
            DriverRepository driverRepository,
            DriverRegistrationRequestRepository driverRegistrationRequestRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            SmsService smsService
    ) {
        this.userRepo = userRepo;
        this.driverRepository = driverRepository;
        this.driverRegistrationRequestRepository = driverRegistrationRequestRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public void registerDriver(DriverRegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepo.findByUsername(username).isPresent()
                || driverRegistrationRequestRepository.existsByUsername(username)) {
            throw new RuntimeException("Nom d'utilisateur déjà utilisé.");
        }

        if (userRepo.findByEmail(email).isPresent()
                || driverRegistrationRequestRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé.");
        }

        DriverRegistrationRequestEntity pendingRequest = new DriverRegistrationRequestEntity();
        pendingRequest.setFullName(request.getFullName().trim());
        pendingRequest.setUsername(username);
        pendingRequest.setEmail(email);
        pendingRequest.setPhone(request.getPhone().trim());
        pendingRequest.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pendingRequest.setStatus(AccountStatus.PENDING);
        pendingRequest.setEmailVerified(false);

        String code = generateVerificationCode();
        pendingRequest.setEmailVerificationCode(code);
        pendingRequest.setEmailVerificationExpiry(OffsetDateTime.now().plusMinutes(10));
        pendingRequest.setCreatedAt(OffsetDateTime.now());

        driverRegistrationRequestRepository.save(pendingRequest);
        emailService.sendVerificationEmail(email, code);
    }

    public List<DriverRegistrationRequestEntity> getPendingDriverRequests() {
        return driverRegistrationRequestRepository.findByStatus(AccountStatus.PENDING);
    }

    public void approveDriverRequest(Long requestId) {
        DriverRegistrationRequestEntity request = driverRegistrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable."));

        if (request.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée.");
        }

        if (!Boolean.TRUE.equals(request.getEmailVerified())) {
            throw new RuntimeException("L'email du chauffeur n'a pas encore été vérifié.");
        }

        if (userRepo.findByUsername(request.getUsername()).isPresent()
                || userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec ce nom d'utilisateur ou cet email existe déjà.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPasswordHash());
        user.setRole("DRIVER");
        user.setMustChangePassword(false);
        user.setIsEnabled(true);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setEmailVerified(true);

        User savedUser = userRepo.save(user);

        Driver driver = new Driver();
        driver.setUser(savedUser);
        driver.setFullName(request.getFullName());
        driver.setPhone(request.getPhone());
        driverRepository.save(driver);

        request.setStatus(AccountStatus.APPROVED);
        driverRegistrationRequestRepository.save(request);

        emailService.sendDriverApprovalEmail(request.getEmail(), request.getFullName());

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            smsService.sendAccountApproved(request.getPhone());
        }
    }

    public void rejectDriverRequest(Long requestId) {
        DriverRegistrationRequestEntity request = driverRegistrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable."));

        if (request.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée.");
        }

        request.setStatus(AccountStatus.REJECTED);
        driverRegistrationRequestRepository.save(request);

        emailService.sendDriverRejectionEmail(request.getEmail(), request.getFullName());

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            smsService.sendAccountRejected(request.getPhone());
        }
    }

    public void approveDriver(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Cet utilisateur n'est pas un chauffeur.");
        }

        user.setAccountStatus(AccountStatus.APPROVED);
        user.setIsEnabled(true);
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
        user.setIsEnabled(false);
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
                user.getEmail(),
                user.getRole(),
                user.getTokenVersion()
        );

        return new AuthResponse(
                token,
                user.getRole(),
                user.getId(),
                user.getEmail(),
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

        String code = generateVerificationCode();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(code);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(OffsetDateTime.now().plusMinutes(30));

        passwordResetTokenRepository.save(resetToken);

        emailService.sendResetPasswordCodeEmail(user.getEmail(), code);

        if ("DRIVER".equalsIgnoreCase(user.getRole())) {
            Driver driver = driverRepository.findByUser_Id(user.getId()).orElse(null);
            if (driver != null && driver.getPhone() != null && !driver.getPhone().isBlank()) {
                smsService.sendDriverResetCode(driver.getPhone(), code);
            }
        }
    }

    public void resendVerificationCode(String email) {
        DriverRegistrationRequestEntity request = driverRegistrationRequestRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Demande introuvable."));

        if (request.getStatus() != AccountStatus.PENDING) {
            throw new RuntimeException("Cette demande n'est plus en attente.");
        }

        String code = generateVerificationCode();
        request.setEmailVerificationCode(code);
        request.setEmailVerificationExpiry(OffsetDateTime.now().plusMinutes(10));

        driverRegistrationRequestRepository.save(request);
        emailService.sendVerificationEmail(email, code);
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