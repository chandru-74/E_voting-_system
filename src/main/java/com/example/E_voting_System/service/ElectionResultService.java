package com.example.E_voting_System.service;

import com.example.E_voting_System.dto.ResultResponse;
import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.entity.ElectionResult;
import com.example.E_voting_System.repository.CandidateRepository;
import com.example.E_voting_System.repository.ElectionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElectionResultService {

    private final ElectionResultRepository electionResultRepository;
    private final CandidateRepository      candidateRepository;

    @Transactional
    public List<ResultResponse> publishResults() {

        List<Candidate> candidates =
                candidateRepository.findByStatus(Candidate.Status.APPROVED); // ← enum

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No approved candidates found");
        }
        if (electionResultRepository.existsByWinnerTrue()) {
            throw new IllegalStateException("Results already published");
        }

        Candidate winner = candidates.stream()
                .max(Comparator.comparingLong(Candidate::getVoteCount))
                .orElseThrow(() -> new IllegalStateException(
                        "Could not determine winner"));

        List<ElectionResult> results = candidates.stream()
                .map(c -> {
                    ElectionResult r = new ElectionResult();
                    r.setCandidate(c);
                    r.setTotalVotes(c.getVoteCount());
                    r.setWinner(c.getId().equals(winner.getId()));
                    r.setPublishedAt(LocalDateTime.now());
                    return r;
                })
                .collect(Collectors.toList());

        electionResultRepository.saveAll(results);

        log.info("Results published — winner: {} ({} votes)",
                winner.getPartyName(), winner.getVoteCount());

        return toResponse(results);
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> getResults() {
        return toResponse(
                electionResultRepository.findAllByOrderByTotalVotesDesc());
    }

    @Transactional(readOnly = true)
    public boolean resultsPublished() {
        return electionResultRepository.existsByWinnerTrue();
    }

    // ── Election Reset ────────────────────────────────────────
    // Deletes every stored ElectionResult row. Since resultsPublished()
    // is just existsByWinnerTrue() against this table, clearing it is
    // exactly the "un-publish" step — publishResults() can be called
    // again afterward without hitting "already published".
    @Transactional
    public void resetPublication() {
        long count = electionResultRepository.count();
        electionResultRepository.deleteAllInBatch();
        log.warn("ELECTION RESET: cleared {} published result row(s) — election un-published", count);
    }

    private List<ResultResponse> toResponse(List<ElectionResult> results) {
        return results.stream()
                .map(r -> new ResultResponse(
                        r.getCandidate().getId(),
                        r.getCandidate().getPartyName(),
                        r.getCandidate().getPartySymbol(),
                        r.getCandidate().getWardName(),
                        r.getTotalVotes(),
                        r.isWinner()
                ))
                .collect(Collectors.toList());
    }
}