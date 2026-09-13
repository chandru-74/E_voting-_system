package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a vote record with blockchain hash for immutability verification.
 *
 * voterId is intentionally NOT stored here — this is what makes the
 * ballot secret. Whether a voter has voted is tracked separately on
 * Voter.hasVoted; this table only records what was voted for and when,
 * with no link back to who cast it.
 */
@Entity
@Table(name = "vote_tracks")
@Getter
@Setter
@NoArgsConstructor
public class VoteTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String voteHash;

    @Column(nullable = false)
    private Long candidateId;

    @Column(nullable = false)
    private LocalDateTime votedAt;
}