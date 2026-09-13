package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.VotingWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

// Checks every 30s whether the active voting window has ended and results
// haven't been published yet. If so, auto-publishes — results go live the
// moment voting closes, with zero admin action required. If there are no
// approved candidates yet (edge case), it just logs and retries next tick.
@Slf4j
@Component
@RequiredArgsConstructor
public class ElectionAutoPublishScheduler {

    private final VotingWindowService   votingWindowService;
    private final ElectionResultService electionResultService;

    @Scheduled(fixedRate = 30_000)
    public void autoPublishIfWindowEnded() {
        Optional<VotingWindow> windowOpt = votingWindowService.getActiveWindow();
        if (windowOpt.isEmpty()) return;

        VotingWindow window = windowOpt.get();
        if (!LocalDateTime.now().isAfter(window.getEndTime())) return;
        if (electionResultService.resultsPublished()) return;

        try {
            electionResultService.publishResults();
            log.info("AUTO-PUBLISH: voting window ended at {} — results published automatically.",
                    window.getEndTime());
        } catch (IllegalStateException e) {
            log.warn("AUTO-PUBLISH skipped: {}", e.getMessage());
        }
    }
}