package com.example.E_voting_System.service;

import com.example.E_voting_System.blockchain.BlockChainService;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.repository.CandidateRepository;
import com.example.E_voting_System.repository.VoteTrackRepository;
import com.example.E_voting_System.repository.VoterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final VoteTrackRepository voteTrackRepository;
    private final CandidateRepository candidateRepository;
    private final VoterRepository     voterRepository;
    private final BlockChainService   blockChainService;
    private final PasswordEncoder     passwordEncoder;

    @Value("${evoting.admin.username}")
    private String adminUsername;

    @Value("${evoting.admin.password-hash}")
    private String adminPasswordHash;

    // ── Authentication ────────────────────────────────────────

    public boolean authenticate(String username, String password) {
        return adminUsername.equals(username)
                && passwordEncoder.matches(password, adminPasswordHash);
    }

    // ── Register ballot card (admin adds a party to a ward) ───

    public boolean registerCandidateRequest(String partyName,
                                            String partySymbol,
                                            String wardName) {
        if (candidateRepository.existsByPartyNameAndWardName(partyName, wardName)) {
            return false;
        }
        Candidate c = new Candidate();
        c.setPartyName(partyName);
        c.setPartySymbol(partySymbol);
        c.setWardName(wardName);
        c.setStatus(Candidate.Status.PENDING);
        c.setVoteCount(0);
        candidateRepository.save(c);
        return true;
    }

    // ── Approve / Reject ──────────────────────────────────────

    public void approveCandidate(Long candidateId) {
        Candidate c = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException(
                        "Candidate not found: " + candidateId));
        c.setStatus(Candidate.Status.APPROVED);
        candidateRepository.save(c);
    }

    public void rejectCandidate(Long candidateId) {
        Candidate c = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException(
                        "Candidate not found: " + candidateId));
        c.setStatus(Candidate.Status.REJECTED);
        candidateRepository.save(c);
    }

    // ── Lists ─────────────────────────────────────────────────

    public List<Candidate> getPendingCandidates() {
        return candidateRepository.findByStatus(Candidate.Status.PENDING);
    }

    public List<Candidate> getApprovedCandidates() {
        return candidateRepository.findByStatus(Candidate.Status.APPROVED);
    }

    // ── Dashboard stats ───────────────────────────────────────

    public long getTotalVoteCount()       { return voteTrackRepository.count(); }
    public long getCandidateCount()       { return getApprovedCandidates().size(); }
    public long getRegisteredVoterCount() { return voterRepository.count(); }
    public long getPendingCandidateCount(){ return getPendingCandidates().size(); }

    // Kept for /admin/audit route, which still shows results explicitly when visited.
    public boolean isChainValid() { return blockChainService.isChainValid(); }

    // NEW: runs the same check on a background thread so the dashboard
    // doesn't wait for it. Fire-and-forget — logs the result, nothing rendered.
    @Async
    public void isChainValidAsync() {
        boolean valid = isChainValid();
        log.info("Background blockchain check completed — valid: {}", valid);
    }
}