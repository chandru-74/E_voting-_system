package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
    }

    public Candidate registerCandidate(Candidate candidate) {
        // minimal validation
        if (candidate.getName() == null || candidate.getName().isBlank()) {
            throw new IllegalArgumentException("Candidate name is required");
        }
        return candidateRepository.save(candidate);
    }

    public Candidate updateCandidate(Long id, Candidate updated) {
        Candidate candidate = getCandidateById(id);
        if (updated.getName() != null && !updated.getName().isBlank()) {
            candidate.setName(updated.getName());
        }
        return candidateRepository.save(candidate);
    }

    public void deleteCandidate(Long id) {
        if (!candidateRepository.existsById(id)) {
            throw new RuntimeException("Candidate not found");
        }
        candidateRepository.deleteById(id);
    }

    /** for UIController dashboards, etc. */
    public long count() {
        return candidateRepository.count();
    }
}
