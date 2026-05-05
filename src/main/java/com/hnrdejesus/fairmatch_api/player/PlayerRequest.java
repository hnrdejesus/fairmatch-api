package com.hnrdejesus.fairmatch_api.player;

import jakarta.validation.constraints.*;

// Defines the request contract for player endpoints.
// Keeping input separate from the entity prevents internal fields (e.g. overall, createdAt)
// from leaking into the API surface and decouples the database schema from the HTTP contract.
public record PlayerRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,

        // Validation is intentionally duplicated from the entity — the DTO is the first
        // line of defense (HTTP layer), the entity is the second (persistence layer).
        @NotNull(message = "Pace is required")
        @Min(0) @Max(100)
        Integer pace,

        @NotNull(message = "Shooting is required")
        @Min(0) @Max(100)
        Integer shooting,

        @NotNull(message = "Passing is required")
        @Min(0) @Max(100)
        Integer passing,

        @NotNull(message = "Dribbling is required")
        @Min(0) @Max(100)
        Integer dribbling,

        @NotNull(message = "Defending is required")
        @Min(0) @Max(100)
        Integer defending,

        @NotNull(message = "Stamina is required")
        @Min(0) @Max(100)
        Integer stamina

) {}