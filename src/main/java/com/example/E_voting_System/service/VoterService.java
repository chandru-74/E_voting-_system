package com.example.E_voting_System.service;

import com.example.E_voting_System.dto.VoterRegisterRequest;
import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.repository.VoterRepository;
import com.example.E_voting_System.util.AadhaarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoterService {

    private final VoterRepository voterRepository;
    private final JavaMailSender  mailSender;
    private final HashService     hashService;   // ← add this

    @Value("${evoting.mail.from}")
    private String fromEmail;

    // Secret pepper mixed into every Aadhaar hash. Stored as an env var
    // (see application.properties), never in the database or source
    // control. Without this, an attacker with DB access could precompute
    // SHA-256 of every plausible 12-digit Aadhaar number (a small search
    // space) and reverse every "hashed" Aadhaar in the table. HMAC with
    // a secret pepper makes that precomputation attack infeasible even
    // with full database access, since the attacker also needs the pepper.
    @Value("${evoting.security.aadhaar-pepper}")
    private String aadhaarPepper;

    // ── Register ──────────────────────────────────────────────

    public Voter register(VoterRegisterRequest request) {
        String aadhaarHash = hashAadhaar(request.getAadhaar());

        if (voterRepository.existsByAadhaarHash(aadhaarHash)) {
            throw new IllegalStateException("This Aadhaar is already registered.");
        }

        Voter voter = new Voter();
        voter.setAadhaarHash(aadhaarHash);
        voter.setName(request.getName());
        voter.setMobile(request.getMobile());
        voter.setEmail(request.getEmail());
        voter.setDob(request.getDob());
        voter.setGender(request.getGender());
        voter.setAddress(request.getAddress());
        voter.setWardName(request.getWardName());

        EligibilityResult result = checkEligibility(request);

        if (result.eligible()) {
            voter.setStatus(Voter.Status.APPROVED);
            log.info("Voter AUTO-APPROVED: {} (ward: {})", request.getName(), request.getWardName());
        } else {
            voter.setStatus(Voter.Status.REJECTED);
            voter.setRejectionReason(result.reason());
            log.info("Voter AUTO-REJECTED: {} — reason: {}", request.getName(), result.reason());
        }

        Voter saved = voterRepository.save(voter);
        sendRegistrationEmail(saved, result);
        return saved;
    }

    // ── Eligibility check ─────────────────────────────────────

    private EligibilityResult checkEligibility(VoterRegisterRequest req) {

        if (req.getDob() == null || req.getDob().isBlank()) {
            return EligibilityResult.fail("Date of birth is required.");
        }
        try {
            LocalDate dob = LocalDate.parse(req.getDob(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age < 18) {
                return EligibilityResult.fail(
                        "Minimum age is 18. Your current age is " + age + ".");
            }
        } catch (Exception e) {
            return EligibilityResult.fail("Invalid date of birth.");
        }

        if (req.getAadhaar() == null || !req.getAadhaar().matches("\\d{12}")) {
            return EligibilityResult.fail("Aadhaar must be exactly 12 digits.");
        }
        if (!AadhaarValidator.isPlausibleAadhaar(req.getAadhaar())) {
            return EligibilityResult.fail(
                    "Aadhaar number is not structurally valid. Please check for typos.");
        }

        if (req.getMobile() == null || !req.getMobile().matches("[6-9]\\d{9}")) {
            return EligibilityResult.fail("Mobile must be a valid 10-digit Indian number.");
        }

        if (req.getWardName() == null || req.getWardName().isBlank()) {
            return EligibilityResult.fail("Ward name is required.");
        }

        return EligibilityResult.pass();
    }

    private record EligibilityResult(boolean eligible, String reason) {
        static EligibilityResult pass()              { return new EligibilityResult(true, null); }
        static EligibilityResult fail(String reason) { return new EligibilityResult(false, reason); }
    }

    // ── Registration email ────────────────────────────────────

    private void sendRegistrationEmail(Voter voter, EligibilityResult result) {
        if (voter.getEmail() == null || voter.getEmail().isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(voter.getEmail());

            if (result.eligible()) {
                msg.setSubject("✅ Registration Approved — E-Voting System");
                msg.setText(
                        "Dear " + voter.getName() + ",\n\n" +
                                "Your voter registration has been AUTOMATICALLY APPROVED.\n\n" +
                                "Login here: http://localhost:9090/voter/login\n" +
                                "Use your Aadhaar + mobile: " + voter.getMobile() + "\n\n" +
                                "— E-Voting System"
                );
            } else {
                msg.setSubject("❌ Registration Rejected — E-Voting System");
                msg.setText(
                        "Dear " + voter.getName() + ",\n\n" +
                                "Your registration was REJECTED.\n\n" +
                                "Reason: " + result.reason() + "\n\n" +
                                "Contact the Election Commission if you believe this is an error.\n\n" +
                                "— E-Voting System"
                );
            }
            mailSender.send(msg);
            log.info("Registration email sent to: {}", voter.getEmail());
        } catch (Exception e) {
            log.warn("Could not send registration email: {}", e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────

    public Voter getById(Long id) {
        return voterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voter not found: " + id));
    }

    public Voter getVoterById(Long id) {
        return getById(id);
    }

    public Voter getByAadhaarHash(String aadhaarHash) {
        return voterRepository.findByAadhaarHash(aadhaarHash)
                .orElseThrow(() -> new RuntimeException("Voter not found for given Aadhaar"));
    }

    // ── Vote ──────────────────────────────────────────────────

    public void markAsVoted(Long voterId) {
        Voter voter = voterRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found: " + voterId));
        voter.setHasVoted(true);
        voterRepository.save(voter);
        log.info("Voter marked as voted — ID: {}", voterId);
    }

    // ── Admin ─────────────────────────────────────────────────

    public List<Voter> getPendingVoters()  { return voterRepository.findByStatus(Voter.Status.PENDING);  }
    public List<Voter> getApprovedVoters() { return voterRepository.findByStatus(Voter.Status.APPROVED); }
    public List<Voter> getRejectedVoters() { return voterRepository.findByStatus(Voter.Status.REJECTED); }

    public void approveVoter(Long id) {
        Voter v = voterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found: " + id));
        v.setStatus(Voter.Status.APPROVED);
        v.setRejectionReason(null);
        voterRepository.save(v);
    }

    public void rejectVoter(Long id, String reason) {
        Voter v = voterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found: " + id));
        v.setStatus(Voter.Status.REJECTED);
        v.setRejectionReason(reason);
        voterRepository.save(v);
    }

    public long countPendingVoters() {
        return voterRepository.countByStatus(Voter.Status.PENDING);
    }

    // ── Election Reset ────────────────────────────────────────

    @Transactional
    public void resetAllVotingStatus() {
        List<Voter> allVoters = voterRepository.findAll();
        allVoters.forEach(v -> v.setHasVoted(false));
        voterRepository.saveAll(allVoters);
        log.warn("ELECTION RESET: hasVoted flag cleared for {} voter(s)", allVoters.size());
    }

    // ── Utility ───────────────────────────────────────────────

    // Replaces the old static sha256(String) method. No longer static,
    // since it needs the injected pepper — see hashing note above.
    public String hashAadhaar(String rawAadhaar) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    aadhaarPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawAadhaar.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Aadhaar hashing failed", e);
        }
    }

    // Atomically claims the vote slot for this voter. Returns true if this
// call successfully claimed it (voter had not voted before), false if
// the voter had already voted — including if a concurrent request won
// the race. This replaces the old hasVoted()-then-markAsVoted() pattern,
// which had a check-then-act race condition.
    @Transactional
    public boolean claimVoteSlot(Long voterId) {
        return voterRepository.claimVoteSlot(voterId) == 1;
    }
    @Transactional
    public void deleteAllVoters() {
        long count = voterRepository.count();
        voterRepository.deleteAllInBatch();
        log.warn("FULL RESET: deleted {} voter row(s) entirely", count);
    }
}