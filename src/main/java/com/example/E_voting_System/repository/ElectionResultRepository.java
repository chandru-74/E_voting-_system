package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.ElectionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectionResultRepository extends JpaRepository<ElectionResult, Long> {

    // Spring reads this method name and auto-generates:
    // SELECT * FROM election_results WHERE winner = true LIMIT 1
    boolean existsByWinnerTrue();

    // SELECT * FROM election_results ORDER BY total_votes DESC
    List<ElectionResult> findAllByOrderByTotalVotesDesc();
}