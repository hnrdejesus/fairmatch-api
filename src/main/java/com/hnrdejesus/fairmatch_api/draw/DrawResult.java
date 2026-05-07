package com.hnrdejesus.fairmatch_api.draw;

import com.hnrdejesus.fairmatch_api.player.PlayerResponse;
import java.util.List;

// overallDifference = Math.abs(sumA - sumB) — the lower the value, the more balanced the draw.
public record DrawResult(
        List<PlayerResponse> teamA,
        List<PlayerResponse> teamB,
        int teamAOverallSum,
        int teamBOverallSum,
        int overallDifference
) {}