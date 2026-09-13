package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Candidate;
import com.example.E_voting_System.entity.ElectionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, Long> {

    Optional<ElectionResult> findByCandidate(Candidate candidate);
}