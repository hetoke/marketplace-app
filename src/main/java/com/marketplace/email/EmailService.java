package com.marketplace.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@marketplace.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your email address";
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String html = buildButtonEmail(
                "Verify your email",
                "<p>Thanks for signing up! Click the button below to verify your email address.</p>",
                "Verify Email",
                verifyUrl,
                "This link expires in 24 hours."
        );
        send(to, subject, html);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset your password";
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String html = buildButtonEmail(
                "Reset your password",
                "<p>We received a request to reset your password. Click the button below to set a new password.</p>",
                "Reset Password",
                resetUrl,
                "This link expires in 1 hour. If you didn't request this, ignore this email."
        );
        send(to, subject, html);
    }

    @Async
    public void sendMfaOtpEmail(String to, String otp) {
        String subject = "Your verification code";
        String html = buildOtpEmail(otp);
        send(to, subject, html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildButtonEmail(String heading, String bodyHtml, String buttonText, String buttonUrl, String footerText) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:40px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:480px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);\">"
                + "<tr><td style=\"padding:32px;\">"
                + "<h1 style=\"font-size:22px;font-weight:700;color:#18181b;margin:0 0 16px;\">" + heading + "</h1>"
                + "<p style=\"font-size:15px;color:#52525b;line-height:1.6;margin:0;\">" + bodyHtml + "</p>"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-top:24px;\">"
                + "<tr><td align=\"center\">"
                + "<a href=\"" + buttonUrl + "\" style=\"display:inline-block;padding:14px 32px;background-color:#18181b;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;\">" + buttonText + "</a>"
                + "</td></tr>"
                + "</table>"
                + "<p style=\"font-size:13px;color:#71717a;margin-top:16px;\">" + footerText + "</p>"
                + "</td></tr>"
                + "</table>"
                + "<p style=\"font-size:12px;color:#a1a1aa;margin-top:24px;\">Marketplace</p>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String buildOtpEmail(String otp) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:40px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:480px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);\">"
                + "<tr><td style=\"padding:32px;\">"
                + "<h1 style=\"font-size:22px;font-weight:700;color:#18181b;margin:0 0 16px;\">Your verification code</h1>"
                + "<p style=\"font-size:15px;color:#52525b;line-height:1.6;margin:0;\">Use the code below to complete your request.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-top:24px;\">"
                + "<tr><td align=\"center\">"
                + "<div style=\"display:inline-block;padding:16px 40px;background-color:#f4f4f5;border-radius:8px;font-family:monospace;font-size:32px;font-weight:700;letter-spacing:8px;color:#18181b;\">" + otp + "</div>"
                + "</td></tr>"
                + "</table>"
                + "<p style=\"font-size:13px;color:#71717a;margin-top:16px;\">This code expires in 5 minutes.</p>"
                + "</td></tr>"
                + "</table>"
                + "<p style=\"font-size:12px;color:#a1a1aa;margin-top:24px;\">Marketplace</p>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}
