package com.example.demo.controller;

import com.example.demo.dto.SendVerificationCodeRequest;
import com.example.demo.dto.VerifyEmailRequest;
import com.example.demo.service.EmailVerificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }
    @GetMapping("/ping")
    public String ping() {
            return "pong";
        }

    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.getEmail(), request.getCode());
        return Map.of(
                "message",
                "Email vérifié avec succès. Votre compte est maintenant en attente de validation par l'administrateur."
        );
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(@RequestBody SendVerificationCodeRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail());
        return Map.of(
                "message",
                "Code de vérification renvoyé avec succès."
        );
    }

    @GetMapping("/verification-test")
    public Map<String, String> verificationTest() {
        return Map.of("message", "verification controller loaded");
    }
}