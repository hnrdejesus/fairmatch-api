package com.hnrdejesus.fairmatch_api.draw;

// Covers two business rule violations: odd player count and non-existent player IDs.
// Mapped to 422 Unprocessable Entity — the request is well-formed but semantically invalid.
public class InvalidDrawException extends RuntimeException {

    public InvalidDrawException(String message) {
        super(message);
    }
}