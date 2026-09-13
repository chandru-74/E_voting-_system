package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.ElectionSetup;
import com.example.E_voting_System.repository.ElectionSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElectionSetupService {

    private static final Long SINGLETON_ID = 1L;

    private final ElectionSetupRepository electionSetupRepository;

    private ElectionSetup getOrCreate() {
        return electionSetupRepository.findById(SINGLETON_ID)
                .orElseGet(() -> {
                    ElectionSetup fresh = new ElectionSetup();
                    fresh.setId(SINGLETON_ID);
                    fresh.setCandidatesLocked(false);
                    return electionSetupRepository.save(fresh);
                });
    }

    public boolean isCandidatesLocked() {
        return getOrCreate().isCandidatesLocked();
    }

    public LocalDateTime getLockedAt() {
        return getOrCreate().getLockedAt();
    }

    @Transactional
    public void lockCandidates() {
        ElectionSetup setup = getOrCreate();
        setup.setCandidatesLocked(true);
        setup.setLockedAt(LocalDateTime.now());
        electionSetupRepository.save(setup);
        log.warn("CANDIDATE SETUP LOCKED — no further nominations, edits, or approvals allowed.");
    }

    @Transactional
    public void unlockCandidates() {
        ElectionSetup setup = getOrCreate();
        setup.setCandidatesLocked(false);
        setup.setLockedAt(null);
        electionSetupRepository.save(setup);
        log.warn("CANDIDATE SETUP UNLOCKED — nominations and edits allowed again.");
    }
}