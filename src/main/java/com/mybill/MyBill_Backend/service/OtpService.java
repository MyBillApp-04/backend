package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;

    private final ConcurrentHashMap<String, OtpDetails> otpStorage = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("mybill.dev@gmail.com", "MyBill Security");
            helper.setTo(email);
            helper.setSubject("MyBill - Device Verification OTP");

            String htmlBody = "<p>Hello,</p>"
                    + "<p>Your MyBill device verification OTP is: <b>" + otp + "</b></p>"
                    + "<p>This OTP is valid for 5 minutes.</p>"
                    + "<p>Device: " + deviceName + "</p>"
                    + "<p>If you did not request this, please ignore this email.</p>";

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
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