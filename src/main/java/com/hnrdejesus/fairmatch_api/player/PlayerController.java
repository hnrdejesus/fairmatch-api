package com.hnrdejesus.fairmatch_api.player;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

// Thin layer — delegates all business logic to PlayerService.
// Responsibilities are limited to: deserializing requests, triggering validation, and serializing responses.
@RestController
@RequestMapping("/players")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    // Returns 201 Created with a Location header pointing to the new resource.
    // UriComponentsBuilder is injected by Spring and builds the URI without hardcoding the host or port.
    // 201 != 200: signals that a resource was created, not just that the request was processed.
    @PostMapping
    public ResponseEntity<PlayerResponse> create(@RequestBody @Valid PlayerRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        PlayerResponse response = service.create(request);
        URI location = uriBuilder
                .path("/players/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // Returns 200 with an empty list when no players exist — an empty collection is not a 404.
    @GetMapping
    public ResponseEntity<List<PlayerResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // PUT replaces the entire resource — all fields are required in the request body.
    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> update(@PathVariable Long id,
                                                 @RequestBody @Valid PlayerRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // Returns 204 No Content — correct REST semantics for a successful delete with no response body.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}