package com.example.E_voting_System.service;

import com.example.E_voting_System.blockchain.BlockChainService;
import com.example.E_voting_System.dto.ResultResponse;
import com.example.E_voting_System.dto.VotePayLoad;
import com.example.E_voting_System.entity.*;
import com.example.E_voting_System.exception.AlreadyVotedException;
import com.example.E_voting_System.exception.ResourceNotFoundException;
import com.example.E_voting_System.repository.*;
import com.example.E_voting_System.stream.SseStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VoteTrackService {

    private final VoterRepository voterRepository;
    private final CandidateRepository candidateRepository;
    private final VoteTrackRepository voteTrackRepository;
    private final BlockChainService blockChainService;
    private final ElectionResultService electionResultService;
    private final HashService hashService;
    private final SseStreamService sseStreamService;


    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // 🔹 Get voter
    public Voter getVoterById(Long id) {
        return voterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voter not found"));
    }

    // 🔥 MAIN METHOD
    @Transactional
    public String castVote(Long voterId, Long candidateId) {

        Voter voter = getVoterById(voterId);

        if (voter.isHasVoted()) {
            throw new AlreadyVotedException("Already voted");
        }

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        // ✅ Save vote
        VoteTrack voteTrack = new VoteTrack();
        voteTrack.setVoter(voter);
        voteTrack.setCandidate(candidate);
        voteTrackRepository.save(voteTrack);

        // ✅ Mark voter
        voter.setHasVoted(true);
        voterRepository.save(voter);

        // ✅ Update result
        electionResultService.updateResult(candidate);

        // ✅ Blockchain
        try {
            VotePayLoad payload = new VotePayLoad(
                    voterId,
                    candidateId,
                    Instant.now(),
                    hashService.sha256Hex(voterId + "-" + candidateId + "-" + Instant.now())
            );

            String json = objectMapper.writeValueAsString(payload);
            blockChainService.addBlock(json);

        } catch (Exception e) {
            throw new RuntimeException("Blockchain write failed", e);
        }

        // 🔥 REAL-TIME UPDATE (SSE)
        sseStreamService.publishEvent(getResultsMap());

        return "Vote successfully cast";
    }

    // 🔹 Results for UI
    public List<ResultResponse> getResults() {
        return candidateRepository.findAll().stream()
                .map(c -> new ResultResponse(
                        c.getId(),   // ✅ ADD THIS (CRITICAL)
                        c.getName(),
                        c.getParty(),
                        voteTrackRepository.countByCandidateId(c.getId())
                ))
                .toList();
    }

    // 🔹 Map for SSE
    public Map<String, Object> getResultsMap() {
        Map<String, Object> map = new HashMap<>();
        for (ResultResponse r : getResults()) {
            map.put(r.getName(), r.getVotes());
        }
        return map;
    }

}