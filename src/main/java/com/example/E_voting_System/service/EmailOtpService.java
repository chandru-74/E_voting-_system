package com.example.E_voting_System.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final JavaMailSender mailSender;

    @Value("${evoting.mail.from}")
    private String fromEmail;

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM   = new SecureRandom();

    public String generateAndSendOtp(String toEmail, String subject, String bodyPrefix) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        otpStore.put(toEmail, otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Tamil Nadu E-Voting — " + subject);
            message.setText(bodyPrefix + otp
                    + "\n\nThis OTP is valid for 10 minutes."
                    + "\nDo not share this with anyone."
                    + "\n\n— Tamil Nadu E-Voting System");
            mailSender.send(message);
            log.info("OTP sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            log.warn("DEV FALLBACK — OTP for {}: {}", toEmail, otp);
        }

        return otp;
    }

    public boolean verifyOtp(String email, String submittedOtp) {
        String stored = otpStore.get(email);
        if (stored != null && stored.equals(submittedOtp.trim())) {
            otpStore.remove(email);
            return true;
        }
        return false;
    }

    /** Used by AdminAuthController to send OTP for ballot card confirmation. */
    public void sendOtp(String email) {
        generateAndSendOtp(email, "Candidate Verification OTP",
                "Your OTP to verify candidate addition: ");
    }

    /** Used by voter OTP flow (email-based fallback). */
    public String sendVoterLoginOtp(String toEmail) {
        return generateAndSendOtp(toEmail, "Login OTP",
                "Your OTP to log in to Tamil Nadu E-Voting: ");
    }
}