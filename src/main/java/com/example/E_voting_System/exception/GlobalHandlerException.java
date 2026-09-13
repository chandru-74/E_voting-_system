package com.example.E_voting_System.exception;

import com.example.E_voting_System.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestControllerAdvice(annotations = RestController.class)  // ✅ FIXED: scope restricted
public class GlobalHandlerException {

    @ExceptionHandler(VoterAlreadyVotedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyVoted(VoterAlreadyVotedException ex,
                                            HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(VoterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(VoterNotFoundException ex,
                                        HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex,
                                                HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex,
                                               HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalState(IllegalStateException ex,
                                            HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException ex,
                                                HttpServletRequest request) {
        return ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex,
                                         HttpServletRequest request) {
        return ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                request.getRequestURI()
        );
    }
}
