# Testing Guide

## Quick Reference

```bash
mvn test                        # Run all tests
mvn test -Dtest=RideServiceImplTest  # Single class
mvn test -Dtest=*RepositoryTest      # Pattern match
```

## Test Profiles

| Layer | Annotation | Context |
|-------|-----------|---------|
| Repository | `@DataJpaTest` + `@ActiveProfiles("test")` | H2 in-memory, real JPA |
| Service | `@ExtendWith(MockitoExtension.class)` | No Spring context |
| Controller | `@WebMvcTest(Controller.class)` | MVC + Security only |

## Non-Obvious Patterns

### CSRF in Controller Tests

All mutating endpoints require `.with(csrf())`:

```java
mockMvc.perform(post(URL)
    .with(csrf())  // Required for POST/PUT/DELETE
    .contentType(MediaType.APPLICATION_JSON)
    .content(json))
```

### Forcing Constraint Validation

H2 validates constraints lazily. Force with `flush()`:

```java
assertThrows(ConstraintViolationException.class, () -> {
    repository.save(invalid);
    entityManager.flush();  // Forces DB constraint check
});
```

### Mocking JPA Specifications

For dynamic queries use `any(Specification.class)`:

```java
when(repository.findAll(any(Specification.class), any(Pageable.class)))
    .thenReturn(new PageImpl<>(rides));
```

### Service Mocks in Controllers

Use `@MockitoBean` (not `@Mock`) in `@WebMvcTest`:

```java
@MockitoBean
private RideService rideService;
```

### Test Data Constants

Import shared constants: `import static com.blablatwo.util.Constants.*;`

Provides: `ID_ONE`, `NON_EXISTENT_ID`, city names, test users, etc.

### Nested Test Classes

Group related tests with `@Nested`:

```java
@Nested
@DisplayName("Create Ride")
class CreateRideTests {
    @BeforeEach void setup() { /* test-specific setup */ }
}
```

### Verify Interactions

Always verify mapper and repository calls:

```java
verify(repository).findById(ID);
verify(mapper).entityToDto(entity);
verify(repository, never()).delete(any());  // Negative verification
```

## Test Data Strategy

- No SQL data loading; each test creates entities in `@BeforeEach`
- H2 uses `create-drop` DDL mode (fresh schema per test class)
- Use builders: `Ride.builder().origin(city).build()`

## Common Assertions

```java
// Grouped assertions
assertAll(
    () -> assertEquals(expected, actual.field()),
    () -> assertNotNull(actual.id())
);

// Exception with message
assertThrows(NoSuchRideException.class,
    () -> service.delete(NON_EXISTENT_ID),
    "Should throw when ride not found");
```

## Authentication in Tests

```java
@WithMockUser  // Basic authenticated user
@WithMockUser(roles = "ADMIN")  // With role
```

## File Locations

- Test config: `src/test/resources/application-test.properties`
- Constants: `src/test/java/com/blablatwo/util/Constants.java`
- Base class: `src/test/java/com/blablatwo/RepositoryTest.java`
