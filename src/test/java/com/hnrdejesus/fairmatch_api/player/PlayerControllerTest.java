package com.hnrdejesus.fairmatch_api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Slice test — loads only the web layer (Controller + GlobalExceptionHandler).
// The Service is mocked, so no database or full application context is needed.
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    // Single source of truth for the base URL — if the mapping changes, only this line needs updating.
    private static final String BASE_URL = "/players";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlayerService service;

    private PlayerRequest validRequest;
    private PlayerResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new PlayerRequest("Cris", 90, 85, 70, 88, 60, 75);
        validResponse = new PlayerResponse(1L, "Cris", 90, 85, 70, 88, 60, 75, 81);
    }

    @Test
    @DisplayName("POST /players should return 201 Created with location header")
    void shouldReturn201WhenCreatingPlayer() throws Exception {
        when(service.create(any(PlayerRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                // endsWith() tests the contract without binding to a specific host, port, or context-path.
                .andExpect(header().string("Location", endsWith("/players/1")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Cris"))
                .andExpect(jsonPath("$.overall").value(81));
    }

    @Test
    @DisplayName("POST /players should return 400 when request is invalid")
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        PlayerRequest invalidRequest = new PlayerRequest("Cris", null, 85, 70, 88, 60, 75);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /players should return 409 when name already exists")
    void shouldReturn409WhenNameAlreadyExists() throws Exception {
        when(service.create(any(PlayerRequest.class)))
                .thenThrow(new DuplicatePlayerException("Cris"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Player with name 'Cris' already exists"));
    }

    @Test
    @DisplayName("GET /players should return 200 with list of players")
    void shouldReturn200WithListOfPlayers() throws Exception {
        PlayerResponse second = new PlayerResponse(2L, "Dani", 80, 75, 85, 78, 70, 80, 79);
        when(service.findAll()).thenReturn(List.of(validResponse, second));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cris"))
                .andExpect(jsonPath("$[1].name").value("Dani"));
    }

    @Test
    @DisplayName("GET /players should return 200 with empty list when no players exist")
    void shouldReturn200WithEmptyList() throws Exception {
        // An empty collection is not a 404 — the /players resource exists, it just has no items.
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /players/{id} should return 200 when player exists")
    void shouldReturn200WhenPlayerFound() throws Exception {
        when(service.findById(1L)).thenReturn(validResponse);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Cris"));
    }

    @Test
    @DisplayName("GET /players/{id} should return 404 when player not found")
    void shouldReturn404WhenPlayerNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Player not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /players/{id} should return 200 when updated successfully")
    void shouldReturn200WhenUpdatedSuccessfully() throws Exception {
        PlayerResponse updated = new PlayerResponse(1L, "Cris", 95, 90, 70, 92, 60, 75, 92);
        when(service.update(eq(1L), any(PlayerRequest.class))).thenReturn(updated);

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pace").value(95))
                .andExpect(jsonPath("$.overall").value(92));
    }

    @Test
    @DisplayName("PUT /players/{id} should return 404 when player not found")
    void shouldReturn404WhenUpdatingNonExistentPlayer() throws Exception {
        when(service.update(eq(99L), any(PlayerRequest.class)))
                .thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(put(BASE_URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("DELETE /players/{id} should return 204 when deleted successfully")
    void shouldReturn204WhenDeletedSuccessfully() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                // 204 No Content carries no body — Content-Type assertion is intentionally omitted.
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(1L);
    }

    @Test
    @DisplayName("DELETE /players/{id} should return 404 when player not found")
    void shouldReturn404WhenDeletingNonExistentPlayer() throws Exception {
        doThrow(new PlayerNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}