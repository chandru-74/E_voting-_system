package com.example.E_voting_System.controller;

import com.example.E_voting_System.common.ApiResponse;
import com.example.E_voting_System.dto.VoterRegistrationDTO;
import com.example.E_voting_System.entity.Voter;
import com.example.E_voting_System.service.VoterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Voter Registration & Management Controller
 *
 * VALIDATION RULES:
 * - Aadhaar: Exactly 12 digits (must pass Verhoeff checksum)
 * - Mobile: Exactly 10 digits, starts with 6-9
 * - Name: 2-100 characters
 * - Email: Valid email format
 */
@Slf4j
@RestController
@RequestMapping("/voter")
@RequiredArgsConstructor
public class VoterController {

    private final VoterService voterService;

    /**
     * Register a new voter with validation
     *
     * @param dto VoterRegistrationDTO with Aadhaar and Mobile
     * @param bindingResult Validation results
     * @return ApiResponse with success/error message
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Voter>> registerVoter(
            @Valid @RequestBody VoterRegistrationDTO dto,
            BindingResult bindingResult) {

        log.info("Voter registration attempt - Aadhaar: {}, Mobile: {}",
                maskAadhaar(dto.getAadhaar()),
                maskMobile(dto.getMobile()));

        // Check binding validation errors
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .findFirst()
                    .orElse("Validation failed");

            log.warn("Validation error: {}", errorMsg);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(errorMsg));
        }

        // Validate Aadhaar checksum
        if (!dto.isValidAadhaar()) {
            log.warn("Invalid Aadhaar checksum for: {}", maskAadhaar(dto.getAadhaar()));
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid Aadhaar number (checksum validation failed)"));
        }

        try {
            Voter voter = voterService.registerVoter(dto);
            log.info("Voter registered successfully - ID: {}", voter.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(voter));

        } catch (IllegalArgumentException ex) {
            log.warn("Registration failed: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage()));

        } catch (Exception ex) {
            log.error("Unexpected error during voter registration", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed: " + ex.getMessage()));
        }
    }

    /**
     * Verify voter before voting
     *
     * @param aadhaar Voter's 12-digit Aadhaar
     * @return ApiResponse with voter info if valid
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Voter>> verifyVoter(
            @RequestParam String aadhaar) {

        log.info("Voter verification attempt - Aadhaar: {}", maskAadhaar(aadhaar));

        // Validate format
        if (!aadhaar.matches("^[0-9]{12}$")) {
            log.warn("Invalid Aadhaar format: {}", maskAadhaar(aadhaar));
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Aadhaar must be exactly 12 digits"));
        }

        try {
            Voter voter = voterService.getVoterByAadhaar(aadhaar);

            // Check if already voted
            if (!voter.canVote()) {
                log.warn("Voter already voted - Aadhaar: {}", maskAadhaar(aadhaar));
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("You have already voted"));
            }

            log.info("Voter verified successfully - ID: {}", voter.getId());
            return ResponseEntity.ok(ApiResponse.success(voter));

        } catch (Exception ex) {
            log.error("Voter verification failed for Aadhaar: {}", maskAadhaar(aadhaar), ex);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Voter not found"));
        }
    }

    /**
     * Get voter by Aadhaar
     */
    @GetMapping("/by-aadhaar/{aadhaar}")
    public ResponseEntity<ApiResponse<Voter>> getVoterByAadhaar(
            @PathVariable String aadhaar) {

        if (!aadhaar.matches("^[0-9]{12}$")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid Aadhaar format"));
        }

        try {
            Voter voter = voterService.getVoterByAadhaar(aadhaar);
            return ResponseEntity.ok(ApiResponse.success(voter));
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Voter not found"));
        }
    }

    /**
     * Get voter by Mobile
     */
    @GetMapping("/by-mobile/{mobile}")
    public ResponseEntity<ApiResponse<Voter>> getVoterByMobile(
            @PathVariable String mobile) {

        if (!mobile.matches("^[6-9][0-9]{9}$")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid mobile format"));
        }

        try {
            Voter voter = voterService.getVoterByMobile(mobile);
            return ResponseEntity.ok(ApiResponse.success(voter));
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Voter not found"));
        }
    }

    // ==========================================
    // UTILITY METHODS (MASKING SENSITIVE DATA)
    // ==========================================

    /**
     * Mask Aadhaar for logging (show only last 4 digits)
     * E.g., 123456789012 -> XXXX6789012
     */
    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return "INVALID";
        }
        return "XXXX" + aadhaar.substring(8);
    }

    /**
     * Mask Mobile for logging (show only last 4 digits)
     * E.g., 9876543210 -> XXXXXX3210
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "INVALID";
        }
        return "XXXXXX" + mobile.substring(6);
    }
}