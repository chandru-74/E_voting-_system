package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.VotingWindow;
import com.example.E_voting_System.repository.VotingWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VotingWindowService {

    private final VotingWindowRepository votingWindowRepository;

    public boolean isVotingOpen() {
        Optional<VotingWindow> window = votingWindowRepository
                .findTopByActiveTrueOrderByIdDesc();
        if (window.isEmpty()) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(window.get().getStartTime())
                && now.isBefore(window.get().getEndTime());
    }

    public Optional<VotingWindow> getActiveWindow() {
        return votingWindowRepository.findTopByActiveTrueOrderByIdDesc();
    }

    public VotingWindow save(VotingWindow window) {
        return votingWindowRepository.save(window);
    }

    public void deactivateAll() {
        votingWindowRepository.findAll()
                .forEach(w -> { w.setActive(false); votingWindowRepository.save(w); });
    }
}