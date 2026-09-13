package com.example.E_voting_System.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HashService {

    // Secret pepper mixed into every ID hash (Aadhaar, candidate ID proof).
    // Stored as an env var, never in the database or source control.
    // Without this, plain SHA-256 of a 12-digit Aadhaar number is
    // precomputable by an attacker with DB access (small search space,
    // fast hash) — HMAC with a secret pepper makes that infeasible even
    // with full database access, since the pepper isn't stored anywhere
    // the attacker can read.
    @Value("${evoting.security.aadhaar-pepper}")
    private String pepper;

    /**
     * Use this for any sensitive government-ID-style value: Aadhaar
     * numbers, candidate ID proof numbers, etc.
     */
    public String hmacSha256Hex(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 hashing failed", e);
        }
    }

    /**
     * Kept for anything that genuinely doesn't need a pepper (e.g. hashing
     * non-sensitive data for a checksum/dedup key, not identity secrets).
     * Do NOT use this for Aadhaar or any other government ID — use
     * hmacSha256Hex() instead.
     */
    public String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}