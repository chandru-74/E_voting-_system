package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.exception.VoterAlreadyVotedException;
import com.example.E_voting_System.exception.VoterNotFoundException;
import com.example.E_voting_System.repository.VoterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoterAuthService {

    private final VoterRepository voterRepository;
    private final HashService     hashService;
    private final OtpService      otpService;

    /**
     * Phase 1: validate Aadhaar + mobile → generate OTP → return pendingVoterId.
     */
    public Long initiateLogin(String aadhaar, String mobile) {

        String aadhaarHash = hashService.hmacSha256Hex(aadhaar);   // ← changed from VoterService.sha256(aadhaar)

        Voter voter = voterRepository.findByAadhaarHash(aadhaarHash)
                .orElseThrow(() -> new VoterNotFoundException(
                        "Aadhaar not found. Please register first."));

        if (voter.getStatus() == Voter.Status.PENDING) {
            throw new VoterNotFoundException(
                    "Your registration is pending admin approval. Please wait.");
        }
        if (voter.getStatus() == Voter.Status.REJECTED) {
            String reason = voter.getRejectionReason() != null
                    ? voter.getRejectionReason()
                    : "Contact admin.";
            throw new VoterNotFoundException(
                    "Your registration was rejected. Reason: " + reason);
        }

        // ── Age verification ─────────────────────────────────────
        if (voter.getDob() != null) {
            int age = Period.between(LocalDate.parse(voter.getDob()), LocalDate.now()).getYears();
            if (age < 18) {
                throw new VoterNotFoundException(
                        "You must be 18 or older to vote. You are currently " + age + " years old.");
            }
        }

        if (!voter.getMobile().equals(mobile)) {
            throw new VoterNotFoundException(
                    "Mobile number does not match our records.");
        }

        if (voter.isHasVoted()) {
            throw new VoterAlreadyVotedException(
                    "You have already cast your vote.", voter.getId());
        }

        otpService.generateAndSend(mobile, voter.getId(), voter.getEmail());
        log.info("OTP sent to voter ID: {}", voter.getId());
        return voter.getId();
    }

    /**
     * Phase 2: verify OTP → return verified voterId.
     */
    public Long verifyOtp(String mobile, String otp) {

        // ← Get voterId FIRST, before verifyOtp removes the entry
        Long voterId = otpService.getVoterId(mobile);

        boolean valid = otpService.verifyOtp(mobile, otp);

        if (!valid) {
            log.warn("Invalid OTP attempt — mobile: {}", maskMobile(mobile));
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        if (voterId == null) {
            log.warn("Voter ID missing after OTP verification — mobile: {}", maskMobile(mobile));
            throw new IllegalStateException("Session expired. Please login again.");
        }

        otpService.clearOtp(mobile);  // already cleared by verifyOtp, but harmless
        log.info("OTP verified — voter ID: {}", voterId);
        return voterId;
    }

    public void clearOtpIfPresent(String mobile) {
        if (mobile != null && !mobile.isBlank()) {
            otpService.clearOtp(mobile);
            log.info("OTP cleared on logout — mobile: {}", maskMobile(mobile));
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "*".repeat(mobile.length() - 4) + mobile.substring(mobile.length() - 4);
    }

    /**
     * Resends OTP to an already-pending login session, without
     * re-validating Aadhaar/mobile (already validated in initiateLogin).
     */
    public void resendOtp(String mobile, Long voterId) {
        Voter voter = voterRepository.findById(voterId)
                .orElseThrow(() -> new VoterNotFoundException("Voter not found."));

        otpService.generateAndSend(mobile, voter.getId(), voter.getEmail());
        log.info("OTP resent to voter ID: {}", voter.getId());
    }
}