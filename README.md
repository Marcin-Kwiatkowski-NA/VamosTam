# Vamigo — Backend

REST API for a ride-sharing platform connecting drivers and passengers across Poland. Built with Java 25 and Spring Boot 4.0.1.

The companion Flutter mobile app lives in [`ride-sharing-frontend`](../ride-sharing-frontend).

## Features

- **Ride search** with spatial proximity filtering via PostGIS
- **Multi-stop rides** — passengers book at a specific board/alight stop pair
- **Booking flow** with driver confirm/reject and optional auto-approve
- **Real-time chat** between drivers and passengers over STOMP WebSocket
- **Push notifications** via Firebase Cloud Messaging
- **Review system** — post-ride ratings with tags and a moderation lifecycle
- **External ride import** — scraper integration with SHA-256 deduplication fingerprint
- **Geocoding** via self-hosted Photon (OpenStreetMap), locations cached and keyed by OSM ID
- **Email** — transactional emails (verification, password reset, review reminders) via Brevo
- **Avatar storage** on S3-compatible object storage (Garage)

## Tech Stack

| | |
|---|---|
| Language / Framework | Java 25, Spring Boot 4.0.1 |
| Database | PostgreSQL + PostGIS (spatial `geography` type via Hibernate Spatial) |
| Authentication | Stateless JWT + Google OAuth2 (Spring Security resource server) |
| Real-time | STOMP WebSocket |
| Mapping | MapStruct 1.6.3 |
| Caching | Caffeine |
| Rate limiting | Bucket4j (per-IP, 20 req/min on auth endpoints) |
| Push | Firebase Admin SDK |
| Email | Brevo |
| Storage | S3-compatible (Garage) |
| Geocoding | Photon / OpenStreetMap |
| Containerization | Docker (multi-stage, eclipse-temurin:25) |
| Testing | JUnit 5, Mockito, Testcontainers (PostGIS), Database Rider |

## Architecture

Strict three-layer architecture enforced by convention:

```
Controller  ──►  Service  ──►  Repository
    │                │
  DTOs           Entities
```

- Controllers handle HTTP only — no business logic, no direct repository access
- Services own all business logic and transactions
- Repositories return entities only — never DTOs
- Domain exceptions map to HTTP status codes via a global `@ControllerAdvice`
- Cross-domain side effects use Spring Application Events with `@TransactionalEventListener`

## Authentication

- JWT access token (20 min) + refresh token (7 days) with rotation
- Google OAuth2 sign-in (Android + web client IDs)
- Email verification flow with 24h expiry and 60s resend cooldown
- Password reset with 1h token expiry
- Brute-force protection: 5 failed login attempts → 15-minute account lock; 20 requests/min per IP → 429

## Running Locally

```bash
mvn clean package    # build + test
mvn spring-boot:run  # starts on :8080
```

Required environment variables: `POSTGRES_PASSWORD`, `JWT_SECRET`, and optionally `BREVO_API_KEY`, `FCM_CREDENTIALS_PATH`, `GARAGE_ACCESS_KEY`, `GARAGE_SECRET_KEY`.

## Testing

```bash
mvn test
```

- Repository tests run against a real PostGIS database via Testcontainers (`imresamu/postgis:18-3.6`)
- Integration tests use `@SpringBootTest` with a full Spring context
- Service tests use Mockito without Spring
- Controller tests use `@WebMvcTest` with Spring Security wired in

## Project Docs

`doc/` contains design notes written during development:

- `architecture.md` — layering rules and anti-patterns
- `patterns.md` — DTOs, MapStruct, JPA Specifications, event-driven patterns
- `security.md` — auth flow, token lifecycle, brute-force protection
- `database.md` — schema conventions
- `test.md` — testing strategy and examples
