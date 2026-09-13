package com.example.E_voting_System.service;

import com.example.E_voting_System.blockchain.BlockChainService;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.entity.ElectionResult;
import com.example.E_voting_System.repository.ElectionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ElectionResultService {

    private final ElectionResultRepository electionResultRepository;
    private final BlockChainService blockChainService;

    public List<ElectionResult> getAllResults() {
        return electionResultRepository.findAll();
    }

    public void updateResult(Candidate candidate) {

        ElectionResult result = electionResultRepository.findByCandidate(candidate)
                .orElseGet(() -> {
                    ElectionResult r = new ElectionResult();
                    r.setCandidate(candidate);
                    r.setVoteCount(0);
                    return r;
                });

        result.setVoteCount(result.getVoteCount() + 1);
        electionResultRepository.save(result);
    }

    public ElectionResult getWinner() {
        return electionResultRepository.findAll()
                .stream()
                .max(Comparator.comparingLong(ElectionResult::getVoteCount))
                .orElseThrow(() -> new RuntimeException("No results found"));
    }

    public Map<String, Long> getElectionResults() {
        try {
            return blockChainService.getElectionResults();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read election results from blockchain", e);
        }
    }
}