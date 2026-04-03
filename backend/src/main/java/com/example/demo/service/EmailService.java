package com.example.demo.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetPasswordEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Réinitialisation de votre mot de passe");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #111827; margin: 0;">WiseTrash</h2>
                    </div>

                    <h3 style="color: #111827;">Réinitialisation du mot de passe</h3>

                    <p style="color: #374151;">Bonjour,</p>

                    <p style="color: #374151;">
                        Nous avons reçu une demande de réinitialisation de votre mot de passe.
                    </p>

                    <p style="color: #374151;">
                        Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :
                    </p>

                    <div style="text-align: center; margin: 28px 0;">
                        <a href="%s"
                           style="background: #10b981; color: #ffffff; padding: 12px 22px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                            Réinitialiser mon mot de passe
                        </a>
                    </div>

                    <p style="color: #374151;">
                        Si le bouton ne fonctionne pas, utilisez ce lien :
                    </p>

                    <p style="word-break: break-all;">
                        <a href="%s">%s</a>
                    </p>

                    <p style="color: #374151;">
                        Ce lien expirera dans <strong>30 minutes</strong>.
                    </p>

                    <p style="color: #374151;">
                        Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.
                    </p>

                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">

                    <p style="color: #6b7280; font-size: 14px;">
                        Cordialement,<br>
                        L'équipe WiseTrash
                    </p>
                </div>
                """.formatted(resetLink, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation", e);
        }
    }
}