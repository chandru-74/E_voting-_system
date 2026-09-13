package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "candidates",
        indexes = {
                // Speeds up the ballot-loading query that runs on every voter
                // dashboard load: findByWardNameAndStatusOrderByIdAsc(ward, APPROVED).
                // Without this, every ballot page load does a full table scan
                // as candidates grow. Composite index covers both the WHERE
                // columns (ward_name, status) — MySQL can use this for the
                // filter directly; the ORDER BY id still uses the primary key,
                // which is fine since id is already indexed as the PK.
                @Index(name = "idx_candidate_ward_status", columnList = "wardName, status"),

                // Speeds up the existing-slot lookup in
                // CandidateService.registerCandidate() —
                // findFirstByPartyNameAndWardName(partyName, wardName) — and the
                // duplicate check in save() —
                // existsByPartyNameAndWardName(partyName, wardName).
                @Index(name = "idx_candidate_party_ward", columnList = "partyName, wardName")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;

    private String dob;

    @Column(length = 64)
    private String idProofHash;

    private String contactNumber;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String assetDeclaration;

    @Column(length = 255)
    private String rejectionReason;

    @Column(nullable = false)
    private String partyName;

    private String partySymbol;

    @Column(nullable = false)
    private String wardName;

    @Column(nullable = false)
    private long voteCount = 0;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
}