package com.example.E_voting_System.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final JavaMailSender mailSender;
    private final SmsService     smsService;
    private final Fast2SmsService  fast2SmsService;

    @Value("${evoting.mail.from}")
    private String fromEmail;

    private record OtpEntry(String otp, Long voterId, LocalDateTime expiresAt) {}

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    // ── Generate + Send ───────────────────────────────────────

    public void generateAndSend(String mobile, Long voterId, String email) {

        if (smsService.isTwilioEnabled()) {
            // Twilio Verify manages the OTP code itself.
            // We still store a dummy entry so verifyOtp() knows this mobile is pending.
            otpStore.put(mobile, new OtpEntry("TWILIO", voterId,
                    LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));

            boolean sent = attemptTwilioSend(mobile);

            if (sent) {
                log.info("OTP dispatched via Twilio Verify for voter ID: {}", voterId);
            } else {
                log.warn("Twilio Verify failed twice for {} — falling back to email", mobile);
                fallbackEmail(mobile, voterId, email);
            }

        } else {
            // In-memory OTP — sent via Fast2SMS if enabled, otherwise
            // Twilio-disabled console log, always emailed if an address exists.
            String otp = String.format("%06d", random.nextInt(1_000_000));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
            otpStore.put(mobile, new OtpEntry(otp, voterId, expiresAt));
            log.info("===== OTP for {}: {} (expires {}) =====",
                    maskMobile(mobile), otp, expiresAt);

            if (fast2SmsService.isFast2SmsEnabled()) {
                fast2SmsService.sendOtp(mobile, otp);
            } else {
                smsService.sendOtp(mobile, otp); // logs to console if disabled
            }

            if (email != null && !email.isBlank()) {
                sendEmail(email, otp);
            }
        }
    }
    public void generateAndSend(String mobile, Long voterId) {
        generateAndSend(mobile, voterId, null);
    }

    public void generateOtp(String mobile, Long voterId) {
        generateAndSend(mobile, voterId, null);
    }

    /**
     * Tries Twilio Verify once, and if that throws or returns false,
     * retries a single time before giving up. This absorbs transient
     * network blips (e.g. a stale pooled connection after the machine
     * wakes from sleep) without permanently failing over to email
     * on a one-off glitch.
     */
    private boolean attemptTwilioSend(String mobile) {
        boolean sent = smsService.sendOtp(mobile, null);
        if (sent) {
            return true;
        }

        log.warn("Twilio Verify attempt 1 failed for {} — retrying once", mobile);
        return smsService.sendOtp(mobile, null);
    }

    // ── Verify ────────────────────────────────────────────────

    public boolean verifyOtp(String mobile, String submittedOtp) {
        OtpEntry entry = otpStore.get(mobile);
        if (entry == null) return false;

        // Twilio Verify path — delegate check to Twilio
        if ("TWILIO".equals(entry.otp())) {
            if (LocalDateTime.now().isAfter(entry.expiresAt())) {
                otpStore.remove(mobile);
                log.warn("Twilio Verify session expired for: {}", maskMobile(mobile));
                return false;
            }
            boolean ok = smsService.verifyCode(mobile, submittedOtp);
            if (ok) otpStore.remove(mobile);
            return ok;
        }

        // In-memory path
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            otpStore.remove(mobile);
            log.warn("OTP expired for: {}", maskMobile(mobile));
            return false;
        }
        boolean match = entry.otp().equals(submittedOtp);
        if (match) otpStore.remove(mobile);
        return match;
    }

    public Long getVoterId(String mobile) {
        OtpEntry entry = otpStore.get(mobile);
        return entry != null ? entry.voterId() : null;
    }

    public void clearOtp(String mobile) {
        otpStore.remove(mobile);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void fallbackEmail(String mobile, Long voterId, String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpStore.put(mobile, new OtpEntry(otp, voterId, expiresAt));
        log.info("===== FALLBACK OTP for {}: {} =====", maskMobile(mobile), otp);
        if (email != null && !email.isBlank()) sendEmail(email, otp);
    }

    private void sendEmail(String email, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(email);
            msg.setSubject("🗳️ Your OTP for Tamil Nadu E-Voting — Valid " + OTP_EXPIRY_MINUTES + " min");
            msg.setText(
                    "தேர்தல் ஆணையம் · Election Commission of Tamil Nadu\n" +
                            "Tamil Nadu Legislative Assembly Election — 2026\n" +
                            "─────────────────────────────────────────────\n\n" +
                            "Your One-Time Password (OTP) for voter login:\n\n" +
                            "        " + otp + "\n\n" +
                            "This code is valid for " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                            "Do not share this OTP with anyone, including election officials.\n\n" +
                            "If you did not request this, you can safely ignore this email.\n\n" +
                            "─────────────────────────────────────────────\n" +
                            "Election Commission of Tamil Nadu · Helpline: 1950\n" +
                            "This is an automated message from the Tamil Nadu E-Voting System."
            );
            mailSender.send(msg);
            log.info("OTP email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "******" + mobile.substring(mobile.length() - 4);
    }
}