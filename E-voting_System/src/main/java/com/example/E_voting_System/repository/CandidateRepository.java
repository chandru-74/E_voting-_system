package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByWard(String ward);
}