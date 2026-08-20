package com.mybill.MyBill_Backend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final JavaMailSender mailSender;
    private final Resend resend;

    private final String provider;
    private final String resendFrom;

    private final ConcurrentHashMap<String, OtpDetails> otpStorage = new ConcurrentHashMap<>();

    public OtpService(
            JavaMailSender mailSender,
            @Value("${app.mail.provider:resend}") String provider,
            @Value("${app.mail.resend.api-key:}") String resendApiKey,
            @Value("${app.mail.resend.from:}") String resendFrom) {
        this.mailSender = mailSender;
        this.provider = provider;
        this.resendFrom = resendFrom;
        this.resend = (resendApiKey != null && !resendApiKey.isBlank())
                ? new Resend(resendApiKey)
                : null;
    }

    static class OtpDetails {
        final String otp;
        final LocalDateTime expiry;

        OtpDetails(String otp) {
            this.otp = otp;
            this.expiry = LocalDateTime.now().plusMinutes(5);
        }
    }

    public void sendDeviceVerificationOtp(String email, String deviceName) {
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        otpStorage.put(email, new OtpDetails(otp));

        String subject = "MyBill - Device Verification OTP";
        String htmlBody = "<p>Hello,</p>"
                + "<p>Your MyBill device verification OTP is: <b>" + otp + "</b></p>"
                + "<p>This OTP is valid for 5 minutes.</p>"
                + "<p>Device: " + deviceName + "</p>"
                + "<p>If you did not request this, please ignore this email.</p>";

        if (resend != null) {
            sendViaResend(email, subject, htmlBody);
        } else {
            sendViaSmtp(email, subject, htmlBody);
        }
    }

    private void sendViaResend(String to, String subject, String htmlBody) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(resendFrom)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();
        try {
            CreateEmailResponse response = resend.emails().send(params);
            log.info("Device verification OTP sent to {} via Resend (id={})", to, response.getId());
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send OTP email via Resend", e);
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("mybill.dev@gmail.com", "MyBill Security");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email via SMTP", e);
        }
    }

    public boolean verifyOtp(String email, String inputOtp) {
        OtpDetails stored = otpStorage.remove(email);
        if (stored == null) {
            return false;
        }
        return stored.otp.equals(inputOtp);
    }
}