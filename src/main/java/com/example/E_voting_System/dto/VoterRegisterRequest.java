package com.example.E_voting_System.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoterRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be under 100 characters")
    private String name;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "\\d{12}", message = "Aadhaar must be exactly 12 digits")
    private String aadhaar;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "\\d{10}", message = "Mobile must be exactly 10 digits")
    private String mobile;

    private String dob;
    private String gender;

    // FIX: email is OPTIONAL per the UI (no required-star, and the client-side
    // JS only validates it when non-empty) -- but the previous server-side fix
    // added @NotBlank, wrongly making it mandatory, and @Pattern alone doesn't
    // skip blank strings the way @NotBlank does (it only skips null).
    // This pattern explicitly allows an empty value ("^$") OR a properly
    // formed address with a real dot+TLD domain -- so blank stays valid,
    // but "user@gmailcom" (no dot) is rejected either way.
    @Email(message = "Please enter a valid email address")
    @Pattern(
            regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "Please enter a valid email address (e.g. name@example.com)"
    )
    private String email;

    private String wardName;
    private String address;
}