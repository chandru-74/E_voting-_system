package com.example.E_voting_System.service;

import com.example.E_voting_System.dto.VoterRegistrationDTO;
import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.repository.VoterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for Voter management
 * Handles registration, verification, and lookup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoterService {

    private final VoterRepository voterRepository;

    /**
     * Register a new voter
     *
     * @param dto VoterRegistrationDTO containing voter details
     * @return Registered Voter entity
     * @throws IllegalArgumentException if Aadhaar or Mobile already exists
     */
    @Transactional
    public Voter registerVoter(VoterRegistrationDTO dto) {

        // Check if Aadhaar already registered
        if (voterRepository.existsByAadhaar(dto.getAadhaar())) {
            throw new IllegalArgumentException("Aadhaar already registered");
        }

        // Check if Mobile already registered
        if (voterRepository.existsByMobile(dto.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered");
        }

        // Create new voter
        Voter voter = new Voter();
        voter.setAadhaar(dto.getAadhaar());
        voter.setMobile(dto.getMobile());
        voter.setName(dto.getName());
        voter.setEmail(dto.getEmail());
        voter.setHasVoted(false);

        Voter savedVoter = voterRepository.save(voter);
        log.info("Voter registered successfully - ID: {}", savedVoter.getId());

        return savedVoter;
    }

    /**
     * Get voter by Aadhaar
     *
     * @param aadhaar 12-digit Aadhaar number
     * @return Voter if found
     * @throws IllegalArgumentException if voter not found
     */
    @Transactional(readOnly = true)
    public Voter getVoterByAadhaar(String aadhaar) {
        return voterRepository.findByAadhaar(aadhaar)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found with Aadhaar: " + aadhaar));
    }

    /**
     * Get voter by Mobile
     *
     * @param mobile 10-digit mobile number
     * @return Voter if found
     * @throws IllegalArgumentException if voter not found
     */
    @Transactional(readOnly = true)
    public Voter getVoterByMobile(String mobile) {
        return voterRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found with Mobile: " + mobile));
    }

    /**
     * Mark voter as voted
     *
     * @param voterId Voter ID
     * @return Updated Voter entity
     */
    @Transactional
    public Voter markVoted(Long voterId) {
        Voter voter = voterRepository.findById(voterId)
                .orElseThrow(() -> new IllegalArgumentException("Voter not found"));

        if (voter.getHasVoted()) {
            throw new IllegalArgumentException("Voter has already voted");
        }

        voter.markAsVoted();
        return voterRepository.save(voter);
    }

    /**
     * Check if voter exists by Aadhaar
     */
    @Transactional(readOnly = true)
    public boolean voterExistsByAadhaar(String aadhaar) {
        return voterRepository.existsByAadhaar(aadhaar);
    }

    /**
     * Check if voter exists by Mobile
     */
    @Transactional(readOnly = true)
    public boolean voterExistsByMobile(String mobile) {
        return voterRepository.existsByMobile(mobile);
    }
}