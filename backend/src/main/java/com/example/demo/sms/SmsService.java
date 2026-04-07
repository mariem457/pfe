package com.example.demo.sms;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private final SmsSender smsSender;

    public SmsService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void sendDriverResetCode(String phone, String code) {
        String msg = "WiseTrash:\n"
                + "Votre code de réinitialisation est : " + code + "\n"
                + "Ce code expire dans 10 minutes.";
        smsSender.send(phone, msg);
    }

    public void sendAccountApproved(String phone) {
        String msg = "WiseTrash:\n"
                + "Votre compte chauffeur a ete valide.\n"
                + "Vous pouvez maintenant vous connecter.";
        smsSender.send(phone, msg);
    }

    public void sendAccountRejected(String phone) {
        String msg = "WiseTrash:\n"
                + "Votre demande d'inscription chauffeur a ete refusee.\n"
                + "Veuillez contacter l'administration pour plus d'informations.";
        smsSender.send(phone, msg);
    }
}