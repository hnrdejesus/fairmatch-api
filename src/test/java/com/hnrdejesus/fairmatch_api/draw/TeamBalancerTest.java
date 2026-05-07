package com.hnrdejesus.fairmatch_api.draw;

import com.hnrdejesus.fairmatch_api.player.PlayerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TeamBalancerTest {

    private TeamBalancer balancer;

    @BeforeEach
    void setUp() {
        balancer = new TeamBalancer();
    }

    // Only overall matters to the algorithm — other attributes are zeroed out to keep tests focused.
    private PlayerResponse player(Long id, String name, int overall) {
        return new PlayerResponse(id, name, 0, 0, 0, 0, 0, 0, overall);
    }

    @Test
    @DisplayName("Should split 10 players into two teams of 5")
    void shouldSplit10PlayersIntoTeamsOf5() {
        List<PlayerResponse> players = List.of(
                player(1L, "A", 80), player(2L, "B", 75),
                player(3L, "C", 70), player(4L, "D", 65),
                player(5L, "E", 60), player(6L, "F", 55),
                player(7L, "G", 50), player(8L, "H", 45),
                player(9L, "I", 40), player(10L, "J", 35)
        );

        DrawResult result = balancer.balance(players);

        assertThat(result.teamA()).hasSize(5);
        assertThat(result.teamB()).hasSize(5);
    }

    @Test
    @DisplayName("Should split 14 players into two teams of 7")
    void shouldSplit14PlayersIntoTeamsOf7() {
        List<PlayerResponse> players = List.of(
                player(1L, "A", 90),  player(2L, "B", 85),
                player(3L, "C", 80),  player(4L, "D", 75),
                player(5L, "E", 70),  player(6L, "F", 65),
                player(7L, "G", 60),  player(8L, "H", 55),
                player(9L, "I", 50),  player(10L, "J", 45),
                player(11L, "K", 40), player(12L, "L", 35),
                player(13L, "M", 30), player(14L, "N", 25)
        );

        DrawResult result = balancer.balance(players);

        assertThat(result.teamA()).hasSize(7);
        assertThat(result.teamB()).hasSize(7);
    }

    @Test
    @DisplayName("Should achieve zero difference when perfect split is possible")
    void shouldAchieveZeroDifferenceWhenPossible() {
        // Total = 200 — optimal split: [100, 0] vs [90, 10] → equal sums of 100
        List<PlayerResponse> players = List.of(
                player(1L, "A", 100),
                player(2L, "B", 90),
                player(3L, "C", 10),
                player(4L, "D", 0)
        );

        DrawResult result = balancer.balance(players);

        assertThat(result.overallDifference()).isZero();
        assertThat(result.teamAOverallSum()).isEqualTo(result.teamBOverallSum());
    }

    @Test
    @DisplayName("Should find optimal split over greedy approach")
    void shouldFindOptimalSplitOverGreedy() {
        // Greedy (interleave by rank): [10, 8] vs [9, 1] → difference 8
        // Backtracking optimum:        [10, 1] vs [9, 8] → difference 6
        List<PlayerResponse> players = List.of(
                player(1L, "A", 10),
                player(2L, "B", 9),
                player(3L, "C", 8),
                player(4L, "D", 1)
        );

        DrawResult result = balancer.balance(players);

        assertThat(result.overallDifference()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should produce most balanced teams with 10 uneven players")
    void shouldProduceMostBalancedTeamsWith10Players() {
        List<PlayerResponse> players = List.of(
                player(1L, "A", 95), player(2L, "B", 88),
                player(3L, "C", 82), player(4L, "D", 76),
                player(5L, "E", 71), player(6L, "F", 65),
                player(7L, "G", 59), player(8L, "H", 52),
                player(9L, "I", 44), player(10L, "J", 38)
        );

        DrawResult result = balancer.balance(players);

        assertThat(result.overallDifference()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should never put the 3 best players on the same team")
    void shouldNeverPutThreeBestPlayersOnSameTeam() {
        // The algorithm has no explicit "separate the best players" rule — it only minimizes
        // the overall difference. This test passes because, with the rating distribution below,
        // grouping the top 3 together creates an imbalance the remaining 7 players cannot compensate.
        //
        // Edge case worth noting: if the top 3 ratings were high enough that grouping them together
        // actually produced the smallest difference, the algorithm would do so — and would be correct.
        // For realistic rating distributions this test is solid.
        List<PlayerResponse> players = List.of(
                player(1L, "Craque1", 95),
                player(2L, "Craque2", 88),
                player(3L, "Craque3", 82),
                player(4L, "D", 70),
                player(5L, "E", 65),
                player(6L, "F", 60),
                player(7L, "G", 55),
                player(8L, "H", 50),
                player(9L, "I", 45),
                player(10L, "J", 40)
        );

        DrawResult result = balancer.balance(players);

        // containsAll checks presence regardless of order — doesNotContainSequence would miss
        // permutations like [Craque1, Craque3, Craque2] and produce a false negative.
        boolean teamAHasAllThree = result.teamA().stream()
                .map(PlayerResponse::name)
                .toList()
                .containsAll(List.of("Craque1", "Craque2", "Craque3"));

        boolean teamBHasAllThree = result.teamB().stream()
                .map(PlayerResponse::name)
                .toList()
                .containsAll(List.of("Craque1", "Craque2", "Craque3"));

        assertThat(teamAHasAllThree).isFalse();
        assertThat(teamBHasAllThree).isFalse();
    }

    @Test
    @DisplayName("Should include all players in the result — no duplicates, no missing")
    void shouldIncludeAllPlayersInResult() {
        List<PlayerResponse> players = List.of(
                player(1L, "A", 80), player(2L, "B", 75),
                player(3L, "C", 70), player(4L, "D", 65),
                player(5L, "E", 60), player(6L, "F", 55),
                player(7L, "G", 50), player(8L, "H", 45),
                player(9L, "I", 40), player(10L, "J", 35)
        );

        DrawResult result = balancer.balance(players);

        // containsExactlyInAnyOrderElementsOf catches both duplicates and missing players —
        // a size-only check would miss cases like a player appearing in both teams.
        List<PlayerResponse> allResultPlayers = Stream.concat(
                result.teamA().stream(),
                result.teamB().stream()
        ).toList();

        assertThat(allResultPlayers).containsExactlyInAnyOrderElementsOf(players);
    }

    @Test
    @DisplayName("Should report correct overall sums for each team")
    void shouldReportCorrectOverallSums() {
        List<PlayerResponse> players = List.of(
                player(1L, "A", 100),
                player(2L, "B", 90),
                player(3L, "C", 10),
                player(4L, "D", 0)
        );

        DrawResult result = balancer.balance(players);

        int realSumA = result.teamA().stream().mapToInt(PlayerResponse::overall).sum();
        int realSumB = result.teamB().stream().mapToInt(PlayerResponse::overall).sum();

        assertThat(result.teamAOverallSum()).isEqualTo(realSumA);
        assertThat(result.teamBOverallSum()).isEqualTo(realSumB);
    }
}