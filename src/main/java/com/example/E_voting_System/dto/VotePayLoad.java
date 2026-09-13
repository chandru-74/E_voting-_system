package com.example.E_voting_System.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VotePayLoad {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;
}