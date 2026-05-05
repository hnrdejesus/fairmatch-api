package com.hnrdejesus.fairmatch_api.player;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Pure unit test — no Spring context, no database, no application startup.
// Instantiates Player directly to validate business logic in isolation.
class PlayerTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // Helper that builds a fully populated Player and triggers overall calculation.
    // @PrePersist only fires through JPA, so calculateOverall() must be called explicitly here.
    private Player buildPlayer(int pace, int shooting, int passing,
                               int dribbling, int defending, int stamina) {
        Player player = Player.builder()
                .name("Test Player")
                .pace(pace)
                .shooting(shooting)
                .passing(passing)
                .dribbling(dribbling)
                .defending(defending)
                .stamina(stamina)
                .overall(0)
                .build();

        player.calculateOverall();
        return player;
    }

    // --- calculateOverall() ---

    @Test
    @DisplayName("Should calculate overall correctly with known values")
    void shouldCalculateOverallCorrectly() {
        // (80×2 + 80×2 + 60 + 80×2 + 60 + 60) / 9 = 660 / 9 = 73 (integer division)
        Player player = buildPlayer(80, 80, 60, 80, 60, 60);

        assertThat(player.getOverall()).isEqualTo(73);
    }

    @Test
    @DisplayName("Should return 100 when all attributes are at maximum")
    void shouldReturnMaxOverallWhenAllAttributesAreMax() {
        Player player = buildPlayer(100, 100, 100, 100, 100, 100);

        assertThat(player.getOverall()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return 0 when all attributes are zero")
    void shouldReturnZeroOverallWhenAllAttributesAreZero() {
        Player player = buildPlayer(0, 0, 0, 0, 0, 0);

        assertThat(player.getOverall()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should weight pace, shooting and dribbling more than other attributes")
    void shouldWeightAttackingAttributesMoreThanDefensive() {
        // Validates the 2× weight decision: a player maxed on pace, shooting and dribbling
        // must outrate a player maxed on the remaining single-weight attributes.
        Player playerA = buildPlayer(100, 100, 0, 100, 0, 0);  // 2× attributes: 600/9 = 66
        Player playerB = buildPlayer(0, 0, 100, 0, 100, 100);  // 1× attributes: 300/9 = 33

        assertThat(playerA.getOverall()).isGreaterThan(playerB.getOverall());
    }

    // --- updateAttributes() ---

    @Test
    @DisplayName("Should recalculate overall immediately after updateAttributes")
    void shouldRecalculateOverallAfterUpdateAttributes() {
        Player player = buildPlayer(80, 80, 60, 80, 60, 60);
        assertThat(player.getOverall()).isEqualTo(73);

        PlayerRequest request = new PlayerRequest(
                "Updated Player", 100, 100, 100, 100, 100, 100
        );

        // Verifies that the entity is never left inconsistent in memory —
        // overall must reflect the new values before any JPA involvement.
        player.updateAttributes(request);

        assertThat(player.getOverall()).isEqualTo(100);
        assertThat(player.getName()).isEqualTo("Updated Player");
    }

    // --- Bean Validation ---

    @Test
    @DisplayName("Should fail validation when pace is null")
    void shouldFailValidationWhenPaceIsNull() {
        // Simulates a request body with a missing "pace" field.
        // Validation must catch the null before calculateOverall() is reached —
        // that unboxing was the original NullPointerException vulnerability.
        Player player = Player.builder()
                .name("Test Player")
                .pace(null)
                .shooting(80).passing(60).dribbling(80).defending(60).stamina(60)
                .overall(0)
                .build();

        Set<ConstraintViolation<Player>> violations = validator.validate(player);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pace"));
    }

    @Test
    @DisplayName("Should fail validation when attribute exceeds maximum")
    void shouldFailValidationWhenAttributeExceedsMax() {
        Player player = Player.builder()
                .name("Test Player")
                .pace(101)
                .shooting(80).passing(60).dribbling(80).defending(60).stamina(60)
                .overall(0)
                .build();

        Set<ConstraintViolation<Player>> violations = validator.validate(player);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pace"));
    }

    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void shouldPassValidationWhenAllFieldsAreValid() {
        Player player = buildPlayer(80, 80, 60, 80, 60, 60);
        player.setName("Cris");

        Set<ConstraintViolation<Player>> violations = validator.validate(player);

        assertThat(violations).isEmpty();
    }
}