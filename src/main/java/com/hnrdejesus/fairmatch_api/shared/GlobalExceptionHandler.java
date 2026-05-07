package com.hnrdejesus.fairmatch_api.shared;

import com.hnrdejesus.fairmatch_api.draw.InvalidDrawException;
import com.hnrdejesus.fairmatch_api.player.DuplicatePlayerException;
import com.hnrdejesus.fairmatch_api.player.PlayerNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;

// Uses Problem Details (RFC 7807 / Spring Boot 3+) instead of a hand-rolled Map response —
// a standardized format that any REST-aware HTTP client already knows how to handle.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlayerNotFoundException.class)
    public ProblemDetail handlePlayerNotFound(PlayerNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicatePlayerException.class)
    public ProblemDetail handleDuplicatePlayer(DuplicatePlayerException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Second line of defense against race conditions — catches the database unique constraint
    // violation that slips past the Service-layer check under concurrent requests.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Data conflict: a record with this value already exists"
        );
    }

    // 422 rather than 400: the request is syntactically valid but violates a business rule
    // (odd player count, non-existent IDs).
    @ExceptionHandler(InvalidDrawException.class)
    public ProblemDetail handleInvalidDraw(InvalidDrawException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // Aggregates all field-level violations into a single response so the client
    // knows every field to fix in one round trip.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed"
        );
        problem.setProperty("errors", errors);
        return problem;
    }
}