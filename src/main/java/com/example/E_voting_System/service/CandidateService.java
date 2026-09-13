package com.example.E_voting_System.service;

import com.example.E_voting_System.dto.CandidateRegisterRequest;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.exception.ResourceNotFoundException;
import com.example.E_voting_System.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final HashService         hashService;

    private static final int MIN_CANDIDATE_AGE = 25;

    // ── Read ──────────────────────────────────────────────────

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findByStatus(Candidate.Status.APPROVED);
    }

    public List<Candidate> getCandidatesByWard(String wardName) {
        // Only real, named, approved candidates are votable — an empty
        // seeded party/symbol slot with nobody attached never appears
        // on the ballot.
        List<Candidate> candidates = candidateRepository
                .findByWardNameAndStatusOrderByIdAsc(wardName, Candidate.Status.APPROVED)
                .stream()
                .filter(c -> c.getCandidateName() != null && !c.getCandidateName().isBlank())
                .toList();
        log.info("Loaded {} approved, named candidate(s) for ward: {}",
                candidates.size(), wardName);
        return candidates;
    }

    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found with id: " + id));
    }

    // ── Vote count ────────────────────────────────────────────

    @Transactional
    public void incrementVoteCount(Long candidateId) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new ResourceNotFoundException(
                    "Cannot increment votes — candidate not found: " + candidateId);
        }
        candidateRepository.incrementVoteCount(candidateId);
        log.info("Vote count incremented for candidate ID: {}", candidateId);
    }

    // ── Admin CRUD (existing "Add Ballot Card" flow) ──────────

    public Candidate save(Candidate candidate) {
        if (candidateRepository.existsByPartyNameAndWardName(
                candidate.getPartyName(), candidate.getWardName())) {
            throw new IllegalStateException(
                    candidate.getPartyName() + " is already registered in ward: "
                            + candidate.getWardName());
        }
        return candidateRepository.save(candidate);
    }

    public void deleteById(Long id) {
        if (!candidateRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Candidate not found with id: " + id);
        }
        candidateRepository.deleteById(id);
    }

    // ── Election Reset ────────────────────────────────────────
    @Transactional
    public void resetAllVoteCounts() {
        List<Candidate> allCandidates = candidateRepository.findAll();
        allCandidates.forEach(c -> c.setVoteCount(0L));
        candidateRepository.saveAll(allCandidates);
        log.warn("ELECTION RESET: vote count zeroed for {} candidate(s)", allCandidates.size());
    }

    // ── Candidate Self-Registration ───────────────────────────
    @Transactional
    public Candidate registerCandidate(CandidateRegisterRequest req) {

        int age = validateAge(req.getDob());
        if (age < MIN_CANDIDATE_AGE) {
            throw new IllegalStateException(
                    "Minimum age to contest is " + MIN_CANDIDATE_AGE +
                            ". Your current age is " + age + ".");
        }

        Optional<Candidate> existingSlot = candidateRepository
                .findFirstByPartyNameAndWardName(req.getPartyName(), req.getWardName());

        Candidate c;
        if (existingSlot.isPresent()) {
            c = existingSlot.get();

            boolean alreadyClaimed =
                    c.getCandidateName() != null && !c.getCandidateName().isBlank()
                            && c.getStatus() != Candidate.Status.REJECTED;

            if (alreadyClaimed) {
                throw new IllegalStateException(
                        req.getPartyName() + " already has a candidate registered (or pending) in "
                                + req.getWardName() + ".");
            }
        } else {
            c = new Candidate();
            c.setPartyName(req.getPartyName());
            c.setPartySymbol(req.getPartySymbol());
            c.setWardName(req.getWardName());
            c.setVoteCount(0);
        }

        c.setCandidateName(req.getCandidateName());
        c.setDob(req.getDob());
        c.setIdProofHash(hashService.hmacSha256Hex(req.getIdProof()));
        c.setContactNumber(req.getContactNumber());
        c.setEmail(req.getEmail());
        c.setAssetDeclaration(req.getAssetDeclaration());
        c.setRejectionReason(null);
        c.setStatus(Candidate.Status.PENDING);

        Candidate saved = candidateRepository.save(c);
        log.info("Candidate application received: {} ({}) for {} — PENDING approval",
                saved.getCandidateName(), saved.getPartyName(), saved.getWardName());
        return saved;
    }

    private int validateAge(String dobStr) {
        try {
            LocalDate dob = LocalDate.parse(dobStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return Period.between(dob, LocalDate.now()).getYears();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date of birth.");
        }
    }

    // ── Admin approval workflow ────────────────────────────────

    public List<Candidate> getPendingCandidates() {
        return candidateRepository.findByStatus(Candidate.Status.PENDING);
    }

    public void approveCandidate(Long id) {
        Candidate c = getCandidateById(id);
        c.setStatus(Candidate.Status.APPROVED);
        c.setRejectionReason(null);
        candidateRepository.save(c);
        log.info("Candidate approved: {} ({}, {})", c.getCandidateName(), c.getPartyName(), c.getWardName());
    }

    public void rejectCandidate(Long id, String reason) {
        Candidate c = getCandidateById(id);
        c.setStatus(Candidate.Status.REJECTED);
        c.setRejectionReason(reason);
        candidateRepository.save(c);
        log.info("Candidate rejected: {} ({}, {}) — {}",
                c.getCandidateName(), c.getPartyName(), c.getWardName(), reason);
    }

    @Transactional
    public void deleteAllCandidates() {
        long count = candidateRepository.count();
        candidateRepository.deleteAllInBatch();
        log.warn("FULL RESET: deleted {} candidate/party row(s) entirely", count);
    }
}