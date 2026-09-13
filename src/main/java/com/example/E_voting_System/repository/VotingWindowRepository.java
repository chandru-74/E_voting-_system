package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.VotingWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VotingWindowRepository extends JpaRepository<VotingWindow, Long> {
    Optional<VotingWindow> findTopByActiveTrueOrderByIdDesc();
}