package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.VoteTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteTrackRepository extends JpaRepository<VoteTrack, Long> {

    Optional<VoteTrack> findByVoteHash(String voteHash);

    @Query("SELECT v.voteHash FROM VoteTrack v ORDER BY v.id DESC LIMIT 1")
    Optional<String> findLatestBlockHash();
}