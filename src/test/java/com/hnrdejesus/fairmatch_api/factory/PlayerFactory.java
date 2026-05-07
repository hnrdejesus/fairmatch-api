package com.hnrdejesus.fairmatch_api.factory;

import com.hnrdejesus.fairmatch_api.player.Player;
import com.hnrdejesus.fairmatch_api.player.PlayerResponse;

// Object Mother — centralizes test data creation.
// If the Player model changes, this is the only place to update.
public class PlayerFactory {

    // Skill attributes are fixed — only overall matters to the balancing algorithm.
    public static Player createWithOverall(Long id, String name, int overall) {
        return Player.builder()
                .id(id)
                .name(name)
                .pace(80)
                .shooting(80)
                .passing(60)
                .dribbling(80)
                .defending(60)
                .stamina(60)
                .overall(overall)
                .build();
    }

    public static PlayerResponse createResponseWithOverall(Long id, String name, int overall) {
        return new PlayerResponse(id, name, 80, 80, 60, 80, 60, 60, overall);
    }
}