package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByWardName(String wardName);

    List<Candidate> findByStatus(Candidate.Status status);

    // FIX: was findByWardNameAndStatus() with no ORDER BY — MySQL doesn't
    // guarantee row order without one, so the ballot's candidate/symbol
    // order could shift between page loads even with identical data.
    // OrderByIdAsc pins it to a stable, deterministic order (registration order).
    List<Candidate> findByWardNameAndStatusOrderByIdAsc(String wardName, Candidate.Status status);

    boolean existsByPartyNameAndWardName(String partyName, String wardName);

    // NEW: finds the single ballot slot for a given party in a given ward,
    // regardless of status. Used by candidate self-registration to attach
    // a real applicant's details onto the existing (likely blank, seeded)
    // row for that party+ward instead of inserting a duplicate row.
    Optional<Candidate> findFirstByPartyNameAndWardName(String partyName, String wardName);

    @Transactional
    @Modifying
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount + 1 WHERE c.id = :candidateId")
    void incrementVoteCount(@Param("candidateId") Long candidateId);
    List<Candidate> findByWardNameIn(Collection<String> wardNames);
}