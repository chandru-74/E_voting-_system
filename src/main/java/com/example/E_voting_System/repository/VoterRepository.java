package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Voter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {

    Optional<Voter> findByAadhaarHash(String aadhaarHash);

    boolean existsByAadhaarHash(String aadhaarHash);

    List<Voter> findByStatus(Voter.Status status);

    long countByStatus(Voter.Status status);

    // Atomic "claim" of the vote slot. Only succeeds (returns 1) if
    // hasVoted was still false at the moment the UPDATE ran — the
    // database enforces this atomically, so two near-simultaneous
    // requests from the same voter can never both succeed, unlike the
    // old "check hasVoted, then separately write" pattern which had a
    // real race window. Returns 0 if the voter had already voted.
    @Modifying
    @Query("UPDATE Voter v SET v.hasVoted = true, v.votedAt = CURRENT_TIMESTAMP " +
            "WHERE v.id = :id AND v.hasVoted = false")
    int claimVoteSlot(@Param("id") Long id);
}