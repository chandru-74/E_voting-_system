package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.ElectionSetup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionSetupRepository extends JpaRepository<ElectionSetup, Long> {
}