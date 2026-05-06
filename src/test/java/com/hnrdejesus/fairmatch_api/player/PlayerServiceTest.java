package com.hnrdejesus.fairmatch_api.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Pure unit test — the repository is mocked, so no database or Spring context is involved.
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository repository;

    @InjectMocks
    private PlayerService service;

    // Captures the exact Player instance passed to repository.save(),
    // allowing assertions on the object the Service built — not just that save() was called.
    @Captor
    private ArgumentCaptor<Player> playerCaptor;

    private PlayerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PlayerRequest("Cris", 90, 85, 70, 88, 60, 75);
    }

    private Player buildSavedPlayer(Long id, PlayerRequest request) {
        Player player = Player.builder()
                .id(id)
                .name(request.name())
                .pace(request.pace())
                .shooting(request.shooting())
                .passing(request.passing())
                .dribbling(request.dribbling())
                .defending(request.defending())
                .stamina(request.stamina())
                .overall(0)
                .build();

        player.calculateOverall();
        return player;
    }

    @Test
    @DisplayName("Should create player successfully and verify object sent to repository")
    void shouldCreatePlayerSuccessfully() {
        when(repository.existsByNameIgnoreCase(validRequest.name())).thenReturn(false);

        Player saved = buildSavedPlayer(1L, validRequest);
        when(repository.save(any(Player.class))).thenReturn(saved);

        PlayerResponse response = service.create(validRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Cris");
        assertThat(response.overall()).isPositive();

        // Inspects the Player object the Service built before persisting —
        // catches mistakes like a missing field in the builder.
        verify(repository).save(playerCaptor.capture());
        Player captured = playerCaptor.getValue();

        assertThat(captured.getName()).isEqualTo("Cris");
        assertThat(captured.getPace()).isEqualTo(90);
        assertThat(captured.getShooting()).isEqualTo(85);
        assertThat(captured.getPassing()).isEqualTo(70);
        assertThat(captured.getDribbling()).isEqualTo(88);
        assertThat(captured.getDefending()).isEqualTo(60);
        assertThat(captured.getStamina()).isEqualTo(75);

        // overall must be 0 here — @PrePersist is JPA's responsibility, not the Service's.
        assertThat(captured.getOverall()).isZero();
    }

    @Test
    @DisplayName("Should throw DuplicatePlayerException when name already exists")
    void shouldThrowDuplicateExceptionWhenNameExists() {
        when(repository.existsByNameIgnoreCase(validRequest.name())).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(DuplicatePlayerException.class)
                .hasMessageContaining("Cris");

        verify(repository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should return all players")
    void shouldReturnAllPlayers() {
        Player p1 = buildSavedPlayer(1L, validRequest);
        Player p2 = buildSavedPlayer(2L,
                new PlayerRequest("Dani", 80, 75, 85, 78, 70, 80));

        when(repository.findAll()).thenReturn(List.of(p1, p2));

        List<PlayerResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(PlayerResponse::name)
                .containsExactly("Cris", "Dani");
    }

    @Test
    @DisplayName("Should return player by id when found")
    void shouldReturnPlayerById() {
        Player player = buildSavedPlayer(1L, validRequest);
        when(repository.findById(1L)).thenReturn(Optional.of(player));

        PlayerResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Cris");
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when player not found by id")
    void shouldThrowNotFoundExceptionWhenPlayerNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(PlayerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should update player successfully and recalculate overall in memory")
    void shouldUpdatePlayerSuccessfully() {
        Player existing = buildSavedPlayer(1L, validRequest);

        // Captured before the update because updateAttributes() mutates the object in place —
        // reading it after would already return the new value.
        int overallBeforeUpdate = existing.getOverall();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        PlayerRequest updateRequest = new PlayerRequest("Cris", 100, 100, 70, 100, 60, 75);

        PlayerResponse response = service.update(1L, updateRequest);

        assertThat(response.pace()).isEqualTo(100);

        // Verifies that the response reflects the recalculated overall — Rich Domain Model in action.
        assertThat(response.overall()).isGreaterThan(overallBeforeUpdate);

        // Dirty checking: Hibernate issues the UPDATE on commit — explicit save() is not expected.
        verify(repository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when updating non-existent player")
    void shouldThrowNotFoundWhenUpdatingNonExistentPlayer() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, validRequest))
                .isInstanceOf(PlayerNotFoundException.class);

        verify(repository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Should throw DuplicatePlayerException when updating to an existing name")
    void shouldThrowDuplicateWhenUpdatingToExistingName() {
        Player existing = buildSavedPlayer(1L, validRequest);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        PlayerRequest requestWithDuplicateName =
                new PlayerRequest("Dani", 90, 85, 70, 88, 60, 75);
        when(repository.existsByNameIgnoreCase("Dani")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, requestWithDuplicateName))
                .isInstanceOf(DuplicatePlayerException.class)
                .hasMessageContaining("Dani");
    }

    @Test
    @DisplayName("Should delete player successfully when exists")
    void shouldDeletePlayerSuccessfully() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        service.delete(1L);

        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw PlayerNotFoundException when deleting non-existent player")
    void shouldThrowNotFoundWhenDeletingNonExistentPlayer() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(PlayerNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}