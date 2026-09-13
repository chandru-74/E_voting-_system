// package com.example.E_voting_System.entity;

package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "election_results")
@Getter
@Setter
@NoArgsConstructor
public class ElectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many results can point to one candidate (Foreign Key in DB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private long totalVotes;

    // true = this candidate won the election
    @Column(nullable = false)
    private boolean winner;

    @Column(nullable = false)
    private LocalDateTime publishedAt;
}