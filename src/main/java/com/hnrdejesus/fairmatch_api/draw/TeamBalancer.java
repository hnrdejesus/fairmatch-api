package com.hnrdejesus.fairmatch_api.draw;

import com.hnrdejesus.fairmatch_api.player.PlayerResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Single responsibility: given a list of players, return two balanced teams.
// Isolated from DrawService to keep the algorithm independently testable.
@Component
public class TeamBalancer {

    // Holds the best state found during recursion.
    // Avoids multiple return values — the recursive calls update this object in place.
    private static class BestDrawState {
        int smallestDifference = Integer.MAX_VALUE;
        List<Integer> bestIndicesA = new ArrayList<>();
    }

    public DrawResult balance(List<PlayerResponse> players) {
        int totalPlayers = players.size();
        int teamSize = totalPlayers / 2;

        // Computed once before recursion — without this, it would be recalculated
        // for each of the ~705k combinations in a 22-player draw.
        int totalOverall = players.stream()
                .mapToInt(PlayerResponse::overall)
                .sum();

        BestDrawState state = new BestDrawState();
        List<Integer> currentCombination = new ArrayList<>();

        // Symmetry breaking: player 0 is always assigned to Team A.
        // Team A=[0,1] vs Team B=[2,3] is the same game as Team A=[2,3] vs Team B=[0,1].
        // Fixing index 0 cuts the search space exactly in half.
        currentCombination.add(0);

        // Each combination is evaluated as soon as it is complete and then discarded —
        // memory usage stays O(n) instead of O(C(n, n/2)).
        findBestCombination(1, totalPlayers, teamSize, currentCombination,
                players, totalOverall, state);

        return buildResult(players, state.bestIndicesA, state.smallestDifference);
    }

    // Recursive backtracking — builds combinations and evaluates them on the fly.
    // start increments on each call to prevent index reuse within a combination.
    private void findBestCombination(int start, int total, int k,
                                     List<Integer> current,
                                     List<PlayerResponse> players,
                                     int totalOverall,
                                     BestDrawState state) {
        if (current.size() == k) {
            int sumA = 0;
            for (Integer index : current) {
                sumA += players.get(index).overall();
            }

            // Team B's sum is derived mathematically — no need to iterate over it separately.
            int difference = Math.abs(sumA - (totalOverall - sumA));

            if (difference < state.smallestDifference) {
                state.smallestDifference = difference;
                // Defensive copy — current will be mutated by backtracking after this point.
                state.bestIndicesA = new ArrayList<>(current);
            }
            return;
        }

        for (int i = start; i < total; i++) {
            current.add(i);
            findBestCombination(i + 1, total, k, current, players, totalOverall, state);
            current.removeLast();
        }
    }

    // Builds the DrawResult from the best Team A indices found by the algorithm.
    private DrawResult buildResult(List<PlayerResponse> players,
                                   List<Integer> teamAIndices,
                                   int difference) {
        List<PlayerResponse> teamA = new ArrayList<>();
        List<PlayerResponse> teamB = new ArrayList<>();
        Set<Integer> indicesA = new HashSet<>(teamAIndices);

        int sumA = 0;
        int sumB = 0;

        // Single pass — players are classified and summed in one iteration.
        for (int i = 0; i < players.size(); i++) {
            PlayerResponse player = players.get(i);
            if (indicesA.contains(i)) {
                teamA.add(player);
                sumA += player.overall();
            } else {
                teamB.add(player);
                sumB += player.overall();
            }
        }

        return new DrawResult(teamA, teamB, sumA, sumB, difference);
    }
}