package com.hnrdejesus.fairmatch_api.draw;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Single endpoint — a draw is a stateless operation, not a persistent resource.
@RestController
@RequestMapping("/draw")
public class DrawController {

    private final DrawService service;

    public DrawController(DrawService service) {
        this.service = service;
    }

    // Returns 200 OK — nothing is persisted, so 201 Created would be semantically incorrect.
    @PostMapping
    public ResponseEntity<DrawResult> draw(@RequestBody @Valid DrawRequest request) {
        return ResponseEntity.ok(service.draw(request));
    }
}