package com.hnrdejesus.fairmatch_api.draw;

import com.hnrdejesus.fairmatch_api.factory.PlayerFactory;
import com.hnrdejesus.fairmatch_api.player.Player;
import com.hnrdejesus.fairmatch_api.player.PlayerRepository;
import com.hnrdejesus.fairmatch_api.player.PlayerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// Pure unit test — TeamBalancer is mocked because the algorithm has its own coverage in TeamBalancerTest.
// This class tests only DrawService orchestration.
@ExtendWith(MockitoExtension.class)
class DrawServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamBalancer teamBalancer;

    @InjectMocks
    private DrawService service;

    private DrawResult fakeDrawResult(List<PlayerResponse> players) {
        int half = players.size() / 2;
        return new DrawResult(
                players.subList(0, half),
                players.subList(half, players.size()),
                100, 100, 0
        );
    }

    @Test
    @DisplayName("Should perform draw successfully with valid even number of players")
    void shouldPerformDrawSuccessfully() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        DrawRequest request = new DrawRequest(ids);

        List<Player> players = List.of(
                PlayerFactory.createWithOverall(1L, "A", 80),
                PlayerFactory.createWithOverall(2L, "B", 75),
                PlayerFactory.createWithOverall(3L, "C", 70),
                PlayerFactory.createWithOverall(4L, "D", 65)
        );

        when(playerRepository.findAllById(ids)).thenReturn(players);

        List<PlayerResponse> playerResponses = players.stream()
                .map(PlayerResponse::from)
                .toList();
        when(teamBalancer.balance(anyList())).thenReturn(fakeDrawResult(playerResponses));

        DrawResult result = service.draw(request);

        assertThat(result).isNotNull();
        assertThat(result.teamA()).hasSize(2);
        assertThat(result.teamB()).hasSize(2);

        // Verifies the single IN query was used — never individual findById calls.
        verify(playerRepository, times(1)).findAllById(ids);
        verify(playerRepository, never()).findById(any());
        verify(teamBalancer, times(1)).balance(anyList());
    }

    // Adding new odd sizes (7, 9, 11) requires only a new entry here — no additional test method.
    private static Stream<List<Long>> provideOddSizedLists() {
        return Stream.of(
                List.of(1L, 2L, 3L),
                List.of(1L, 2L, 3L, 4L, 5L)
        );
    }

    @ParameterizedTest(name = "Should throw when odd number of players: {0}")
    @MethodSource("provideOddSizedLists")
    @DisplayName("Should throw InvalidDrawException when number of players is odd")
    void shouldThrowWhenNumberOfPlayersIsOdd(List<Long> oddSizedList) {
        DrawRequest request = new DrawRequest(oddSizedList);

        assertThatThrownBy(() -> service.draw(request))
                .isInstanceOf(InvalidDrawException.class)
                .hasMessageContaining("must be even")
                .hasMessageContaining(String.valueOf(oddSizedList.size()));

        verify(playerRepository, never()).findAllById(anyList());
        verify(teamBalancer, never()).balance(anyList());
    }

    @Test
    @DisplayName("Should throw InvalidDrawException when some player IDs do not exist")
    void shouldThrowWhenPlayerIdsDoNotExist() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        DrawRequest request = new DrawRequest(ids);

        // findAllById returns only the found ones — IDs 3 and 4 are missing.
        when(playerRepository.findAllById(ids)).thenReturn(List.of(
                PlayerFactory.createWithOverall(1L, "A", 80),
                PlayerFactory.createWithOverall(2L, "B", 75)
        ));

        assertThatThrownBy(() -> service.draw(request))
                .isInstanceOf(InvalidDrawException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("4");

        verify(teamBalancer, never()).balance(anyList());
    }

    @Test
    @DisplayName("Should list all missing IDs in the exception message")
    void shouldListAllMissingIds() {
        List<Long> ids = List.of(1L, 2L, 99L, 100L);
        DrawRequest request = new DrawRequest(ids);

        when(playerRepository.findAllById(ids)).thenReturn(List.of(
                PlayerFactory.createWithOverall(1L, "A", 80),
                PlayerFactory.createWithOverall(2L, "B", 75)
        ));

        assertThatThrownBy(() -> service.draw(request))
                .isInstanceOf(InvalidDrawException.class)
                .hasMessageContaining("99")
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("Should use findAllById — never individual findById calls")
    void shouldUseBatchQueryInsteadOfIndividualCalls() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        DrawRequest request = new DrawRequest(ids);

        List<Player> players = List.of(
                PlayerFactory.createWithOverall(1L, "A", 80),
                PlayerFactory.createWithOverall(2L, "B", 75),
                PlayerFactory.createWithOverall(3L, "C", 70),
                PlayerFactory.createWithOverall(4L, "D", 65)
        );

        when(playerRepository.findAllById(ids)).thenReturn(players);
        when(teamBalancer.balance(anyList())).thenReturn(
                fakeDrawResult(players.stream().map(PlayerResponse::from).toList())
        );

        service.draw(request);

        verify(playerRepository, times(1)).findAllById(ids);
        verify(playerRepository, never()).findById(any());
    }
}