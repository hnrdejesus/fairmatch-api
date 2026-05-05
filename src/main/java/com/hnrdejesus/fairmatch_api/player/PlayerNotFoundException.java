package com.hnrdejesus.fairmatch_api.player;

// Domain exception — represents a business situation, not an HTTP concept.
// The Service throws this without any knowledge of the web layer.
// HTTP translation (404) is handled exclusively by GlobalExceptionHandler.
public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(Long id) {
        super("Player not found with id: " + id);
    }
}