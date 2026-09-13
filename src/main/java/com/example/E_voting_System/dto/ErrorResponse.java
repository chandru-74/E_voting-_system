package com.example.E_voting_System.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final String  message;
    private final int     status;
    private final String  path;
    private final Instant timestamp;

    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(message, status, path, Instant.now());
    }
}