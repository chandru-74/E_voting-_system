package com.example.E_voting_System.dto;

// Aggregated across ALL wards for one party — used for the
// "overall party totals" views (live overview + final results).
//
// `wardsWon` = number of wards where this party had the strict highest
// vote count in that ward. A tie for first place within a single ward
// counts as won by no one (not credited to either tied party).
//
// `leading` marks the party with the most wards won overall — ties in
// wards-won are broken by total votes across all wards. This is
// deliberately NOT the same as "most total votes overall": a party can
// win more individual wards while collecting fewer raw votes than a
// party that piles up huge margins in just one or two large wards.
public record PartyResultResponse(
        String  partyName,
        String  partySymbol,
        long    totalVotes,
        int     wardsWon,
        boolean leading
) {}