package com.example.E_voting_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoterRegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Father name is required")
    private String fatherName;

    @NotBlank(message = "Booth place is required")
    private String boothPlace;

    // Improvement: Aadhaar is always 12 digits — validate it
    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;

    // Improvement: Indian mobile numbers are 10 digits
    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 10, message = "Phone must be 10 digits")
    @Pattern(regexp = "\\d{10}", message = "Phone must contain only digits")
    private String phoneNumber;
}