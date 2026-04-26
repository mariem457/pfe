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
                    <p style="color: #374151;">Nous avons reçu une demande de réinitialisation de votre mot de passe.</p>
                    <p style="color: #374151;">Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <a href="%s" style="background: #10b981; color: #ffffff; padding: 12px 22px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                            Réinitialiser mon mot de passe
                        </a>
                    </div>
                    <p style="color: #374151;">Si le bouton ne fonctionne pas, utilisez ce lien :</p>
                    <p style="word-break: break-all;"><a href="%s">%s</a></p>
                    <p style="color: #374151;">Ce lien expirera dans <strong>30 minutes</strong>.</p>
                    <p style="color: #374151;">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.</p>
                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">
                    <p style="color: #6b7280; font-size: 14px;">Cordialement,<br>L'équipe WiseTrash</p>
                </div>
                """.formatted(resetLink, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation", e);
        }
    }

    public void sendResetPasswordCodeEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Code de réinitialisation du mot de passe");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #111827; margin: 0;">WiseTrash</h2>
                    </div>
                    <h3 style="color: #111827;">Réinitialisation du mot de passe</h3>
                    <p style="color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Nous avons reçu une demande de réinitialisation de votre mot de passe.</p>
                    <p style="color: #374151;">Utilisez le code suivant pour continuer :</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <span style="background: #10b981; color: #ffffff; padding: 14px 24px; border-radius: 10px; font-weight: bold; display: inline-block; font-size: 22px; letter-spacing: 4px;">
                            %s
                        </span>
                    </div>
                    <p style="color: #374151;">Ce code expirera dans <strong>30 minutes</strong>.</p>
                    <p style="color: #374151;">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.</p>
                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">
                    <p style="color: #6b7280; font-size: 14px;">Cordialement,<br>L'équipe WiseTrash</p>
                </div>
                """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email du code de réinitialisation", e);
        }
    }

    public void sendDriverApprovalEmail(String to, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Votre compte chauffeur a été approuvé");

            String html = """

            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #111827; margin: 0;">WiseTrash</h2>
                    </div>
                    <h3 style="color: #111827;">Demande acceptée</h3>
                    <p style="color: #374151;">Bonjour %s,</p>
                    <p style="color: #374151;">Votre demande d'inscription en tant que <strong>chauffeur</strong> a été <span style="color: #16a34a; font-weight: bold;">acceptée</span> par l'administrateur.</p>
                    <p style="color: #374151;">Si vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administration pour plus d'informations.</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <span style="background: #16a34a; color: #ffffff; padding: 12px 22px; border-radius: 8px; font-weight: bold; display: inline-block;">
                            Demande acceptée
                        </span>
                    </div>
                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">
                    <p style="color: #6b7280; font-size: 14px;">Cordialement,<br>L'équipe WiseTrash</p>
                </div>
                """.formatted(fullName);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email d'approbation chauffeur", e);
        }
    }

    public void sendDriverRejectionEmail(String to, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Votre demande d'inscription a été refusée");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #111827; margin: 0;">WiseTrash</h2>
                    </div>
                    <h3 style="color: #111827;">Demande refusée</h3>
                    <p style="color: #374151;">Bonjour %s,</p>
                    <p style="color: #374151;">Votre demande d'inscription en tant que <strong>chauffeur</strong> a été <span style="color: #dc2626; font-weight: bold;">refusée</span> par l'administrateur.</p>
                    <p style="color: #374151;">Si vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administration pour plus d'informations.</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <span style="background: #ef4444; color: #ffffff; padding: 12px 22px; border-radius: 8px; font-weight: bold; display: inline-block;">
                            Demande refusée
                        </span>
                    </div>
                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">
                    <p style="color: #6b7280; font-size: 14px;">Cordialement,<br>L'équipe WiseTrash</p>
                </div>
                """.formatted(fullName);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de refus chauffeur", e);
        }
    }

    public void sendVerificationEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Vérification de votre adresse email");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #111827; margin: 0;">WiseTrash</h2>
                    </div>
                    <h3 style="color: #111827;">Vérification de l'email</h3>
                    <p style="color: #374151;">Bonjour,</p>
                    <p style="color: #374151;">Merci pour votre inscription. Utilisez le code ci-dessous pour vérifier votre adresse email :</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <span style="background: #10b981; color: #ffffff; padding: 14px 24px; border-radius: 10px; font-weight: bold; display: inline-block; font-size: 22px; letter-spacing: 4px;">
                            %s
                        </span>
                    </div>
                    <p style="color: #374151;">Ce code expirera dans <strong>10 minutes</strong>.</p>
                    <hr style="margin: 24px 0; border: none; border-top: 1px solid #e5e7eb;">
                    <p style="color: #6b7280; font-size: 14px;">Cordialement,<br>L'équipe WiseTrash</p>
                </div>
                """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de vérification", e);
        }
    }
}