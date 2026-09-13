package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Voter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Voter entity
 * Provides CRUD and custom query operations
 */
@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {

    /**
     * Find voter by Aadhaar
     */
    Optional<Voter> findByAadhaar(String aadhaar);

    /**
     * Find voter by Mobile
     */
    Optional<Voter> findByMobile(String mobile);

    /**
     * Check if voter exists with given Aadhaar
     */
    boolean existsByAadhaar(String aadhaar);

    /**
     * Check if voter exists with given Mobile
     */
    boolean existsByMobile(String mobile);

    /**
     * Find all voters who have voted
     */
    long countByHasVotedTrue();

    /**
     * Find all voters who haven't voted
     */
    long countByHasVotedFalse();
}