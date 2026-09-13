package com.example.E_voting_System.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// FIX 2: Had @Getter + @Data + manual getter methods all at once
// @Data already generates getters, setters, equals, hashCode, toString
// @Getter on top of @Data is redundant
// Manual getters on top of both is triply redundant — 3 ways of doing same thing
// Cleaned down to just @Getter + @AllArgsConstructor (no setters needed for a response DTO)
@Setter
@Getter
@AllArgsConstructor
@ToString
public class ResultResponse {

    private Long id;
    private String name;
    private String party;
    private long votes;
}