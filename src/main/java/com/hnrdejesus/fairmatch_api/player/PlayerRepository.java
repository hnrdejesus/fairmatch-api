package com.hnrdejesus.fairmatch_api.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Spring generates the full implementation at runtime from JpaRepository<Player, Long>.
// Custom methods should only be added when the required query cannot be derived
// from Spring Data's method naming convention.
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Spring Data derives the query from the method name:
    // SELECT COUNT(*) > 0 FROM players WHERE LOWER(name) = LOWER(?1)
    boolean existsByNameIgnoreCase(String name);
}