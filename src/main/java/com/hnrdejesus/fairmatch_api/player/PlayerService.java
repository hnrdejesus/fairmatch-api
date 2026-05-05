package com.hnrdejesus.fairmatch_api.player;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PlayerResponse create(PlayerRequest request) {
        // Fail-fast check: returns a user-friendly 409 before touching the database.
        // Does not protect against race conditions — that is enforced by the unique
        // constraint on the database and handled in GlobalExceptionHandler.
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicatePlayerException(request.name());
        }

        Player player = Player.builder()
                .name(request.name())
                .pace(request.pace())
                .shooting(request.shooting())
                .passing(request.passing())
                .dribbling(request.dribbling())
                .defending(request.defending())
                .stamina(request.stamina())
                .overall(0)
                .build();

        Player saved = repository.save(player);
        return PlayerResponse.from(saved);
    }

    // readOnly = true: signals read intent and disables Hibernate dirty checking for a performance gain
    @Transactional(readOnly = true)
    public List<PlayerResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(PlayerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse findById(Long id) {
        Player player = repository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        return PlayerResponse.from(player);
    }

    @Transactional
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = repository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));

        // Only checks for name conflict if the name actually changed,
        // so a full update with the same name does not trigger a false 409.
        boolean nameChanged = !player.getName().equalsIgnoreCase(request.name());
        if (nameChanged && repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicatePlayerException(request.name());
        }

        player.updateAttributes(request);

        // Hibernate dirty checking detects the mutations above and issues the UPDATE
        // automatically on transaction commit — no explicit save() needed.
        return PlayerResponse.from(player);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new PlayerNotFoundException(id);
        }
        repository.deleteById(id);
    }
}