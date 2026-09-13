package com.example.E_voting_System.dto;

// Record is a modern Java 16+ feature — generates constructor,
// getters, equals, hashCode, toString automatically.
// Perfect for immutable response objects (no setters needed).
public record ResultResponse(
        Long   candidateId,
        String partyName,
        String partySymbol,
        String wardName,
        long   totalVotes,
        boolean winner
) {}