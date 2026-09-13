package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voters",
        indexes = {
                // Speeds up VoterRepository.findByStatus(Voter.Status) — used by
                // the admin's pending/approved/rejected voter management lists.
                // Without this, each of those three lists does a full table scan
                // as the voter count grows.
                @Index(name = "idx_voter_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Voter {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as SHA-256 hash for privacy
    @Column(nullable = false, unique = true, length = 64)
    private String aadhaarHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    // Extra fields for SIR-style registration
    private String email;
    private String dob;        // "YYYY-MM-DD"
    private String gender;
    private String address;
    private String wardName;

    @Column(nullable = false)
    private boolean hasVoted = false;

    // NEW: when this voter cast their vote. Set atomically together with
    // hasVoted in VoterRepository.claimVoteSlot(). This records WHEN a
    // voter voted, not WHAT they voted for — it does not touch or weaken
    // the ballot-secrecy guarantee (VoteTrack still has no link back to
    // the voter). Null until the voter actually votes.
    private LocalDateTime votedAt;

    // ── NEW: approval workflow ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    // Admin rejection reason (optional)
    @Column(length = 255)
    private String rejectionReason;
}