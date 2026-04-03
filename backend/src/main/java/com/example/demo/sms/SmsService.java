package com.example.demo.sms;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private final SmsSender smsSender;

    public SmsService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void sendDriverCredentials(String phone, String username, String tempPassword) {
        String msg = "Compte Chauffeur:\n"
                + "Username: " + username + "\n"
                + "Password: " + tempPassword + "\n"
                + "Please change your password after login.";
        smsSender.send(phone, msg);
    }

    public void sendDriverResetCode(String phone, String code) {
        String msg = "WiseTrash:\n"
                + "Votre code de réinitialisation est: " + code + "\n"
                + "Ce code expire dans 10 minutes.";
        smsSender.send(phone, msg);
    }
}