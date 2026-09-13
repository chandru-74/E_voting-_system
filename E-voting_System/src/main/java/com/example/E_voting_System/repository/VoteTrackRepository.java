package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.VoteTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteTrackRepository extends JpaRepository<VoteTrack, Long> {

    long countByCandidateId(Long candidateId); // ✅ cleaner & recommended
}