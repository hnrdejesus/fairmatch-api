package com.hnrdejesus.fairmatch_api.player;

// Defines the response contract for player endpoints.
// Returning a dedicated DTO instead of the entity directly avoids exposing internal
// fields, prevents JPA lazy-loading serialization issues, and decouples the database
// schema from the API contract.
public record PlayerResponse(
        Long id,
        String name,
        Integer pace,
        Integer shooting,
        Integer passing,
        Integer dribbling,
        Integer defending,
        Integer stamina,
        Integer overall
) {
    // Static factory — centralizes entity-to-DTO conversion.
    // Any structural change to this mapping has a single point of update.
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getPace(),
                player.getShooting(),
                player.getPassing(),
                player.getDribbling(),
                player.getDefending(),
                player.getStamina(),
                player.getOverall()
        );
    }
}