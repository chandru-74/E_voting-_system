package com.example.E_voting_System.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String aadhaarHash;

    @Column(unique = true)
    private String phoneHash;

    private boolean hasVoted = false;
}