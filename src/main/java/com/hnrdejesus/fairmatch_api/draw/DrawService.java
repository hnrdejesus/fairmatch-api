package com.hnrdejesus.fairmatch_api.draw;

import com.hnrdejesus.fairmatch_api.player.Player;
import com.hnrdejesus.fairmatch_api.player.PlayerResponse;
import com.hnrdejesus.fairmatch_api.player.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DrawService {

    private final PlayerRepository playerRepository;
    private final TeamBalancer teamBalancer;

    public DrawService(PlayerRepository playerRepository, TeamBalancer teamBalancer) {
        this.playerRepository = playerRepository;
        this.teamBalancer = teamBalancer;
    }

    @Transactional(readOnly = true)
    public DrawResult draw(DrawRequest request) {
        if (request.playerIds().size() % 2 != 0) {
            throw new InvalidDrawException(
                    "Number of players must be even. Received: " + request.playerIds().size()
            );
        }

        // Single IN query instead of N findById calls.
        List<Player> players = playerRepository.findAllById(request.playerIds());

        // findAllById silently ignores missing IDs — without this check, an invalid ID
        // would reduce the player count and produce a draw with unequal or incomplete teams.
        if (players.size() != request.playerIds().size()) {
            List<Long> foundIds = players.stream()
                    .map(Player::getId)
                    .toList();
            List<Long> missingIds = request.playerIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new InvalidDrawException("Players not found with IDs: " + missingIds);
        }

        List<PlayerResponse> playerResponses = players.stream()
                .map(PlayerResponse::from)
                .toList();

        return teamBalancer.balance(playerResponses);
    }
}