package com.example.E_voting_System.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vote data stored in blockchain
 * Represents a single vote transaction
 */
@Data
@NoArgsConstructor

public class VotePayLoad {

    /**
     * ID of the candidate being voted for
     */
    @NotNull(message = "Candidate ID cannot be null")
    @Min(value = 1, message = "Candidate ID must be positive")
    private Long candidateId;

    /**
     * Name of the candidate (optional)
     */
    @Size(max = 255, message = "Candidate name cannot exceed 255 characters")
    private String candidateName;

    /**
     * ID of the voter (optional for privacy)
     */
    @Min(value = 1, message = "Voter ID must be positive")
    private Long voterId;

    /**
     * Timestamp when vote was cast (epoch milliseconds)
     * Should be non-null for audit trails
     */
    @NotNull(message = "Timestamp cannot be null")
    @Min(value = 0, message = "Timestamp cannot be negative")
    private Long timestamp;

    /**
     * Create a VotePayLoad with required fields
     */
    public VotePayLoad(Long candidateId, Long timestamp) {
        this.candidateId = candidateId;
        this.timestamp = timestamp;
    }

    /**
     * Create a VotePayLoad with candidate and voter info
     */
    public VotePayLoad(Long candidateId, String candidateName, Long voterId, Long timestamp) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.voterId = voterId;
        this.timestamp = timestamp;
    }

    /**
     * Validate vote payload
     */
    public boolean isValid() {
        return candidateId != null
                && candidateId > 0
                && timestamp != null
                && timestamp > 0;
    }

    /**
     * Convert to JSON string representation for blockchain storage
     * Used by ObjectMapper
     */
    @Override
    public String toString() {
        return "VotePayLoad{" +
                "candidateId=" + candidateId +
                ", candidateName='" + candidateName + '\'' +
                ", voterId=" + voterId +
                ", timestamp=" + timestamp +
                '}';
    }
}