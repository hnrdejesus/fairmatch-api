package com.hnrdejesus.fairmatch_api.draw;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DrawRequest(
        @NotEmpty(message = "Player list cannot be empty")
        @Size(min = 4, max = 22, message = "Number of players must be between 4 and 22")
        // Even number is enforced separately in DrawService as a business rule.
        List<Long> playerIds
) {}