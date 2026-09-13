package com.example.E_voting_System.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private record OtpEntry(String otp, Instant createdAt, Long voterId) {}

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final long OTP_TTL_SECONDS = 300;

    // ✅ FIXED METHOD
    public String generateOtp(String phone, Long voterId) {
        String otp = String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));

        otpStore.put(phone, new OtpEntry(otp, Instant.now(), voterId));

        System.out.println("OTP for " + phone + " : " + otp);

        return otp;
    }

    public boolean verifyOtp(String phone, String otp) {
        OtpEntry entry = otpStore.get(phone);

        if (entry == null) return false;

        boolean expired = Instant.now().isAfter(
                entry.createdAt().plusSeconds(OTP_TTL_SECONDS)
        );

        if (expired) {
            otpStore.remove(phone);
            return false;
        }

        if (entry.otp().equals(otp)) {
            return true;
        }



        return false;
    }

    public Long getVoterId(String phone) {
        OtpEntry entry = otpStore.get(phone);
        return (entry != null) ? entry.voterId() : null;
    }

    public void clearOtp(String phone) {
        otpStore.remove(phone);
    }
}