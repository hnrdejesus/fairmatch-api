# FairMatch API

A REST API for fair team balancing in amateur football. Manage your squad and let the algorithm split teams based on each player's attributes.

---

## Table of Contents

- [How It Works](#how-it-works)
- [Technical Decisions](#technical-decisions)
- [Stack](#stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Endpoints](#endpoints)
- [Usage Examples](#usage-examples)
- [Project Structure](#project-structure)

---

## How It Works

1. Register players with FIFA-style attributes (pace, shooting, passing, dribbling, defending, stamina)
2. The system automatically calculates each player's `overall` using weighted attributes
3. Before the match, send the IDs of the players present
4. The algorithm splits them into two teams with the smallest possible overall difference

### Overall Calculation

```
overall = (pace*2 + shooting*2 + passing + dribbling*2 + defending + stamina) / 9
```

Pace, shooting and dribbling carry double weight as they have the most impact in a match.

### Balancing Algorithm

The draw engine uses **combinatorial backtracking** to find the optimal split, not a random shuffle. It evaluates all possible combinations and returns the teams with the smallest overall difference.

Three optimizations ensure performance with up to 22 players:

- **Single total calculation:** the total overall is computed once before the loop, not on every iteration
- **On-the-fly evaluation:** combinations are evaluated as they are generated, with no memory accumulation
- **Symmetry breaking:** player 0 is fixed to Team A, cutting the search space in half

---

## Technical Decisions

### Flat Architecture (YAGNI)

The project uses a flat, domain-based architecture instead of layered Clean Architecture with multiple abstractions. Each module (`player`, `draw`) contains all its classes: entity, DTOs, repository, service, controller and exceptions, with no unnecessary interfaces.

The YAGNI principle (*You Aren't Gonna Need It*) drove this decision. Creating an `IPlayerRepository` when only one database exists adds cognitive overhead with no real benefit.

The only exception is `TeamBalancer`, which is isolated from `DrawService` for testability: the algorithm needs to be tested independently, without Spring or a database.

### Rich Domain Model

The `overall` calculation and attribute mutation live inside the `Player` entity, not in the service layer. This ensures the object never exists in an inconsistent state in memory, even before JPA acts via `@PrePersist`.

```java
public void updateAttributes(PlayerRequest request) {
    this.name = request.name();
    // ... other fields
    calculateOverall(); // recalculates immediately, does not wait for @PreUpdate
}
```

### Domain Exceptions

`PlayerService` and `DrawService` have no knowledge of HTTP. They throw domain exceptions (`PlayerNotFoundException`, `DuplicatePlayerException`, `InvalidDrawException`) that are translated to HTTP status codes by the `GlobalExceptionHandler` with `@RestControllerAdvice`.

### Problem Details (RFC 7807)

Errors follow the RFC 7807 international standard via Spring Boot 3+ `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Player not found with id: 99",
  "instance": "/players/99"
}
```

### Race Condition Protection

Duplicate name validation uses two layers of defense:

1. **Fail-fast in the service** - `existsByNameIgnoreCase()` before save, with a friendly message
2. **Database constraint** - `unique = true` on the `name` column, guaranteeing uniqueness even under concurrency

### N+1 Query Prevention

`DrawService` uses `findAllById()` instead of `findById()` inside a loop, resulting in a single `SELECT ... WHERE id IN (...)` query regardless of how many players are requested.

---

## Stack

- **Java 21**
- **Spring Boot 3.5** - Web, Data JPA, Validation, DevTools
- **PostgreSQL 16** via Docker
- **Hibernate 6** with Dirty Checking and `@Transactional`
- **JUnit 5 + Mockito** - unit tests and controller tests with MockMvc
- **Lombok**

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/hnrdejesus/fairmatch-api.git
cd fairmatch-api
```

### 2. Start the database

```bash
docker compose up -d
```

> PostgreSQL will be available at `localhost:5433`.
> The project uses port 5433 to avoid conflicts with any local PostgreSQL installation on port 5432.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 4. Run the tests

```bash
./mvnw test
```

---

## Endpoints

### Players

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/players` | Create a player |
| `GET` | `/players` | List all players |
| `GET` | `/players/{id}` | Get player by ID |
| `PUT` | `/players/{id}` | Update player |
| `DELETE` | `/players/{id}` | Delete player |

### Draw

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/draw` | Generate balanced teams |

---

## Usage Examples

### Create a player

```http
POST /players
Content-Type: application/json

{
  "name": "Cris",
  "pace": 90,
  "shooting": 85,
  "passing": 70,
  "dribbling": 88,
  "defending": 60,
  "stamina": 75
}
```

```json
{
  "id": 1,
  "name": "Cris",
  "pace": 90,
  "shooting": 85,
  "passing": 70,
  "dribbling": 88,
  "defending": 60,
  "stamina": 75,
  "overall": 81
}
```

### Generate teams

```http
POST /draw
Content-Type: application/json

{
  "playerIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
}
```

```json
{
  "teamA": [...],
  "teamB": [...],
  "teamAOverallSum": 412,
  "teamBOverallSum": 410,
  "overallDifference": 2
}
```

### Error responses

| Status | When it occurs |
|--------|----------------|
| `400 Bad Request` | Invalid JSON or missing required fields |
| `404 Not Found` | Player not found |
| `409 Conflict` | Player name already exists |
| `422 Unprocessable Entity` | Odd number of players or non-existent ID in draw request |

---

## Project Structure

```
src/main/java/com/hnrdejesus/fairmatch_api/
├── player/
│   ├── Player.java                   # Entity with overall calculation
│   ├── PlayerRequest.java            # Input DTO
│   ├── PlayerResponse.java           # Output DTO
│   ├── PlayerRepository.java         # Spring Data JPA
│   ├── PlayerService.java            # Business logic
│   ├── PlayerController.java         # REST endpoints
│   ├── PlayerNotFoundException.java  # Domain exception
│   └── DuplicatePlayerException.java # Domain exception
├── draw/
│   ├── TeamBalancer.java             # Balancing algorithm
│   ├── DrawRequest.java              # Input DTO
│   ├── DrawResult.java               # Output DTO
│   ├── DrawService.java              # Draw orchestration
│   ├── DrawController.java           # REST endpoint
│   └── InvalidDrawException.java     # Domain exception
└── shared/
    └── GlobalExceptionHandler.java   # Exception to HTTP translation
```