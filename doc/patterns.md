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
com.blablatwo.ride/       → Controller, Service, Repository, Mapper, DTOs
com.blablatwo.city/       → ...
com.blablatwo.traveler/   → ...
com.blablatwo.config/     → Security, cross-cutting
com.blablatwo.exceptions/ → GlobalExceptionHandler
```