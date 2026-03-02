# Project Patterns

## DTOs (Java Records)

- Request: `*CreationDto` with validation annotations
- Response: `*ResponseDto` with nested DTOs
- Keep in same package as feature

## MapStruct

```java
@Mapper(componentModel = "spring", uses = {CityMapper.class})
```

- Abstract class for complex mappers (multiple @Mapping)
- Interface for simple mappers
- Ignore: id, version
- Set defaults: `constant="OPEN"`, `expression="java(Instant.now())"`

## JPA Specifications (complex queries)

```java
public static Specification<Ride> hasStatus(RideStatus status) {
    return (root, query, cb) -> status == null ? null
        : cb.equal(root.get("rideStatus"), status);
}
// Compose: Specification.where(hasStatus(OPEN)).and(originContains("Berlin"))
```

## Exception Naming → HTTP Status

| Pattern | Status |
|---------|--------|
| NoSuch*, *NotFoundException | 404 |
| Already*, Duplicate*, *Conflict | 409 |
| *NotAllowed | 403 |
| *Validation, *NotBookable | 400 |

GlobalExceptionHandler returns ProblemDetail (RFC 7807).

## Package Structure

```
com.vamigo
├── auth/           → AuthController, AuthService, JWT, email verification, Google OAuth
├── config/         → Security, JPA, Caching, Async, WebSocket (STOMP), Firebase
├── domain/         → AbstractTrip base class, Status enums, shared utilities
├── dto/            → Cross-domain DTOs (UserCardDto)
├── email/          → Brevo transactional email service
├── exceptions/     → GlobalExceptionHandler + custom exception types
├── location/       → Location entity, PhotonClient, LocationResolutionService
├── messaging/      → Conversation, Message entities, STOMP chat
├── notification/   → Notification, DeviceToken, FCM push, schedulers
├── report/         → Report entity, abuse reporting
├── review/         → Review entity, rating/tags, lifecycle schedulers
├── ride/           → Ride, RideStop, RideBooking, external rides (scraper API)
├── search/         → SearchConfig, GeoUtils (spatial queries)
├── seat/           → Seat (passenger search), external seats (scraper API)
├── user/           → UserAccount, UserProfile, UserStats, capabilities
├── vehicle/        → Vehicle entity
└── utils/          → Helpers
```

## Event-Driven Patterns

```java
// Publish domain events after state changes
applicationEventPublisher.publishEvent(new BookingRequestedEvent(booking));

// Listen cross-domain (runs after transaction commits)
@TransactionalEventListener
public void onBookingConfirmed(BookingConfirmedEvent event) { ... }
```

## Scheduler Patterns

```java
@Scheduled(fixedRateString = "${business-rules.booking.expiry-check-interval-seconds}000")
public void expirePendingBookings() { ... }
```

Configuration in `business-rules.yml`.