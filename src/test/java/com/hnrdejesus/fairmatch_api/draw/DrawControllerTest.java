package com.hnrdejesus.fairmatch_api.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnrdejesus.fairmatch_api.factory.PlayerFactory;
import com.hnrdejesus.fairmatch_api.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DrawController.class)
@Import(GlobalExceptionHandler.class)
class DrawControllerTest {

    private static final String BASE_URL = "/draw";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DrawService service;

    private DrawResult validResult;

    @BeforeEach
    void setUp() {
        List<com.hnrdejesus.fairmatch_api.player.PlayerResponse> teamA = List.of(
                PlayerFactory.createResponseWithOverall(1L, "A", 80),
                PlayerFactory.createResponseWithOverall(4L, "D", 65)
        );
        List<com.hnrdejesus.fairmatch_api.player.PlayerResponse> teamB = List.of(
                PlayerFactory.createResponseWithOverall(2L, "B", 75),
                PlayerFactory.createResponseWithOverall(3L, "C", 70)
        );
        validResult = new DrawResult(teamA, teamB, 145, 145, 0);
    }

    @Test
    @DisplayName("POST /draw should return 200 with balanced teams")
    void shouldReturn200WithBalancedTeams() throws Exception {
        DrawRequest request = new DrawRequest(List.of(1L, 2L, 3L, 4L));
        when(service.draw(any(DrawRequest.class))).thenReturn(validResult);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.teamA").isArray())
                .andExpect(jsonPath("$.teamB").isArray())
                .andExpect(jsonPath("$.teamA.length()").value(2))
                .andExpect(jsonPath("$.teamB.length()").value(2))
                .andExpect(jsonPath("$.teamAOverallSum").value(145))
                .andExpect(jsonPath("$.teamBOverallSum").value(145));
        // overallDifference is a business rule concern — covered by TeamBalancerTest.
    }

    @Test
    @DisplayName("POST /draw should return 400 when player list is empty")
    void shouldReturn400WhenPlayerListIsEmpty() throws Exception {
        DrawRequest request = new DrawRequest(List.of());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /draw should return 400 when player list has less than 4 players")
    void shouldReturn400WhenLessThan4Players() throws Exception {
        DrawRequest request = new DrawRequest(List.of(1L, 2L));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("POST /draw should return 422 when number of players is odd")
    void shouldReturn422WhenOddNumberOfPlayers() throws Exception {
        // 5 players passes Bean Validation (size >= 4) but violates the even-number business rule.
        DrawRequest request = new DrawRequest(List.of(1L, 2L, 3L, 4L, 5L));

        when(service.draw(any(DrawRequest.class)))
                .thenThrow(new InvalidDrawException(
                        "Number of players must be even. Received: 5"
                ));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Number of players must be even. Received: 5"
                ));
    }

    @Test
    @DisplayName("POST /draw should return 422 when player IDs do not exist")
    void shouldReturn422WhenPlayerIdsDoNotExist() throws Exception {
        DrawRequest request = new DrawRequest(List.of(1L, 2L, 99L, 100L));

        when(service.draw(any(DrawRequest.class)))
                .thenThrow(new InvalidDrawException(
                        "Players not found with IDs: [99, 100]"
                ));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Players not found with IDs: [99, 100]"
                ));
    }
}