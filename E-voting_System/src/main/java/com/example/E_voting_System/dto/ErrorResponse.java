package com.example.E_voting_System.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private int status;
    private String path;
    private Instant timestamp;
}