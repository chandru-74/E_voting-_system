package com.example.E_voting_System.exception;

public class AlreadyVotedException extends RuntimeException {

    public AlreadyVotedException(String message) {
        super(message);
    }
}