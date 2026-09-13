package com.example.E_voting_System.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateRegisterRequest {

    @NotBlank(message = "Candidate name is required.")
    private String candidateName;

    @NotBlank(message = "Date of birth is required.")
    private String dob; // "YYYY-MM-DD" from an HTML date input

    @NotBlank(message = "ID proof number is required.")
    private String idProof; // raw value — hashed before storage, never persisted as-is

    @NotBlank(message = "Contact number is required.")
    private String contactNumber;

    private String email; // optional

    @NotBlank(message = "Please select a party.")
    private String partyName;

    private String partySymbol;

    @NotBlank(message = "Please select a constituency.")
    private String wardName;

    // Renamed from "manifesto" — this is now a mandatory public asset &
    // liabilities declaration (cash, property, vehicles, jewellery,
    // investments, loans, etc.), matching real EC nomination disclosure
    // requirements, not an optional platform statement.
    @NotBlank(message = "Asset declaration is required.")
    private String assetDeclaration;
}