package com.hnrdejesus.fairmatch_api;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Represents a player registered in the system.
 *
 * <p>Attributes follow a FIFA-style rating scale (0–100) and are used by the
 * {@code TeamBalancer} algorithm to produce statistically balanced teams.
 *
 * <p>The {@code overall} rating is derived automatically via JPA lifecycle
 * hooks — it is never accepted as user input.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must have at most 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    // FIFA-style attributes — rated on a 0 to 100 scale.

    @NotNull(message = "Pace is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer pace;

    @NotNull(message = "Shooting is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer shooting;

    @NotNull(message = "Passing is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer passing;

    @NotNull(message = "Dribbling is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer dribbling;

    @NotNull(message = "Defending is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer defending;

    @NotNull(message = "Stamina is required")
    @Min(0) @Max(100)
    @Column(nullable = false)
    private Integer stamina;

    /**
     * Weighted overall rating — computed internally, never sourced from user input.
     *
     * <p>Pace, shooting, and dribbling carry double weight (2×) because they
     * have the greatest impact on small-sided game outcomes.
     * Weight distribution: 2+2+1+2+1+1 = 9.
     */
    @Column(nullable = false)
    private Integer overall;

    /**
     * Recalculates the player's overall rating before every INSERT and UPDATE.
     *
     * <p>Keeping this logic inside the entity follows the Rich Domain Model
     * pattern — business rules live with the data they operate on, rather than
     * being scattered across service or controller layers.
     */
    @PrePersist
    @PreUpdate
    public void calculateOverall() {
        this.overall = (pace * 2 + shooting * 2 + passing
                + dribbling * 2 + defending + stamina) / 9;
    }
}
