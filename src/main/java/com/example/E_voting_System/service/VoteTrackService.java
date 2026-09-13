package com.example.E_voting_System.service;

import com.example.E_voting_System.entity.VoteTrack;
import com.example.E_voting_System.exception.ResourceNotFoundException;
import com.example.E_voting_System.repository.VoteTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteTrackService {

    private final VoteTrackRepository voteTrackRepository;
    private final HashService         hashService;

    // voterId still flows in here only to make the hash unique/unguessable
    // (it's mixed into the raw string, same as before) — it is NOT stored
    // on the entity. Nothing persisted can be traced back to the voter.
    @Transactional
    public VoteTrack recordVote(Long voterId, Long candidateId) {
        String raw      = voterId + ":" + candidateId + ":" + System.nanoTime();
        String voteHash = hashService.sha256Hex(raw);

        VoteTrack track = new VoteTrack();
        track.setVoteHash(voteHash);
        track.setCandidateId(candidateId);
        track.setVotedAt(LocalDateTime.now());

        VoteTrack saved = voteTrackRepository.save(track);
        log.info("Vote recorded — hash: {}...", voteHash.substring(0, 8));
        return saved;
    }

    public VoteTrack findByHash(String hash) {
        return voteTrackRepository.findByVoteHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vote not found for hash: " + hash));
    }

    @Transactional
    public void clearAllVotes() {
        long count = voteTrackRepository.count();
        voteTrackRepository.deleteAllInBatch();
        log.warn("ELECTION RESET: cleared {} vote record(s)", count);
    }
}