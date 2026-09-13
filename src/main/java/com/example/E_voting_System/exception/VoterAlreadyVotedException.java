package com.example.E_voting_System.exception;

/**
 * Thrown when a voter attempts to log in but has already cast their vote.
 *
 * The voterId field allows the AuthController to redirect directly to the
 * already-voted details page without a second database lookup.
 */
public class VoterAlreadyVotedException extends RuntimeException {

    private final Long voterId;

    // ── Constructor used when voterId is known (preferred) ───────────────────
    public VoterAlreadyVotedException(String message, Long voterId) {
        super(message);
        this.voterId = voterId;
    }

    // ── Fallback constructor — voterId will be null ───────────────────────────
    public VoterAlreadyVotedException(String message) {
        super(message);
        this.voterId = null;
    }

    public Long getVoterId() {
        return voterId;
    }
}