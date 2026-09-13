package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each result belongs to one candidate
    @OneToOne
    @JoinColumn(name = "candidate_id", unique = true)
    private Candidate candidate;

    private long voteCount;
}