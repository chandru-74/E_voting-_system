package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "election_setup")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ElectionSetup {

    @Id
    private Long id; // always 1 — single settings row for the whole election

    private boolean candidatesLocked;

    private LocalDateTime lockedAt;
}