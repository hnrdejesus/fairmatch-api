package com.hnrdejesus.fairmatch_api.player;

// Domain exception — represents a business rule violation (unique player name).
// HTTP translation (409 Conflict) is handled exclusively by GlobalExceptionHandler.
public class DuplicatePlayerException extends RuntimeException {

    public DuplicatePlayerException(String name) {
        super("Player with name '" + name + "' already exists");
    }
}