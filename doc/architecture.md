# Backend Architecture Rules

## Intent

Strict layering for codegen: thin HTTP controllers, use-case services, persistence-only repositories.

## Golden Rules (MUST)

- Controllers: HTTP only (routing, DTOs, validation, status codes)
- Services: business logic (use-cases, transactions, domain validation)
- Repositories: persistence only (queries, entities)
- Controllers MUST NOT call repositories
- Services MUST NOT use web types (ResponseEntity, HttpServletRequest)
- Repositories MUST NOT return DTOs

## Controllers

- One per resource, delegate to one service method
- Use @Valid for shape validation
- Let GlobalExceptionHandler handle errors (no inline try/catch)
- Return 201+Location for POST, 204 for DELETE

## Services

- Interface + Impl, explicit use-case methods (createRide, bookRide)
- @Transactional (readOnly=true for queries)
- Throw domain exceptions (NoSuchRideException, RideFullException)
- Return DTOs via MapStruct, pass primitives not Principal

## Repositories

- Extend JpaRepository + JpaSpecificationExecutor for complex queries
- Return entities only, use Spring Data query methods

## Pre-merge Checklist

- [ ] Controller: @Valid, no business logic, no repository calls
- [ ] Service: owns @Transactional, throws domain exceptions
- [ ] Response DTO hides entity internals
- [ ] New exceptions handled in GlobalExceptionHandler