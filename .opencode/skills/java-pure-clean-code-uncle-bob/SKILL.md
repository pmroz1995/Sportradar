---
name: Java Pure Clean Code
description: Write and review clean Plain Java 17+ code using Craftsmanship rules (SOLID, YAGNI, guard clauses, composition over inheritance, one top-level type per file, no magic literals, null-safe guards) and AssertJ tests in Arrange/Act/Assert style. Use for implementing, refactoring, or reviewing Java code, especially in Plain Java Maven projects.
---

# Java Pure Clean Code (Uncle Bob)

## Purpose

Enforce absolute code cleanliness, reduce technical debt, and apply rigorous
object-oriented design using only native Java mechanisms. This skill targets
Plain Java 17+ with no code-generating libraries.

## When to use this skill

- Implementing a new Java library, package, or module.
- Refactoring existing Java code toward clean code and SOLID.
- Reviewing Java code before a pull request or code review.
- Writing unit tests that must follow Arrange/Act/Assert with AssertJ.

## Non-negotiable rules

1. **Plain Java 17+.** Use only native language mechanisms and explicit
   object wiring; no code-generating annotations.
2. **SOLID.** Every class has one reason to change; depend on abstractions,
   not concrete details.
3. **YAGNI.** Do not build abstractions, layers, or extension points that are
   not needed today. An interface is justified only by DIP or by test seams,
   not by speculation.
4. **Guard clauses.** Replace the arrow anti-pattern with early returns or
   early throws.
5. **Composition over inheritance.** Inject collaborators through the
   constructor; do not extend a class to reuse behavior.
6. **`private final` fields whenever possible.** Prefer immutability.
7. **No `final` on method parameters.** Parameters are already effectively
   local; the keyword adds noise. Never reassign a parameter.
8. **No `Optional<T>` as a method parameter.** Use `Optional` only as the
   return type of a query that may have no result.
9. **Tests use `// Arrange`, `// Act`, `// Assert`** and AssertJ assertions.
10. **One top-level type per file.** Never nest a record, enum, or helper class
    inside another type; extract it to its own file.
11. **No magic literals.** Every meaningful string or number becomes a
    `private static final` constant with an intention-revealing name.
12. **Null safety.** Guard reference parameters at the boundary, and compare
    string literals from the literal side: `"text".equals(value)`.

## Methods and functions

- **Small and smaller.** A method should rarely exceed 10–15 lines. It must do
  one thing (Do One Thing) and do it well.
- **Single Level of Abstraction (SLAP).** Do not mix business logic with
  low-level string, collection, or bit manipulation inside one method. Extract
  well-named private methods instead.
- **At most 2–3 arguments.** Zero arguments are ideal. One or two are
  acceptable. Three require justification. More than three require a dedicated
  parameter object or record.
- **Command-Query Separation.** A method either changes state (command) or
  returns data (query), not both. Do not hide side effects behind a getter-like
  name.
- **Intention-revealing names.** Use `daysSinceCreation`, not `d`. Use
  `matchesInProgress`, not `list`.

### Guard clauses instead of the arrow anti-pattern

Avoid deep nesting and the "arrow" shape created by stacked `if` blocks:

```java
// Avoid: arrow anti-pattern
public void updateScore(Match match, int home, int away) {
    if (match != null) {
        if (match.isLive()) {
            if (home >= 0 && away >= 0) {
                // real work buried three levels deep
            }
        }
    }
}
```

Use guard clauses that fail fast and keep the happy path flat:

```java
// Prefer: guard clauses
public void updateScore(Match match, int home, int away) {
    requireLive(match);
    requireNonNegative(home, away);

    // real work at one level of abstraction
}

private void requireLive(Match match) {
    if (match == null) {
        throw new IllegalArgumentException("match must not be null");
    }
    if (!match.isLive()) {
        throw new IllegalStateException("match is not in progress");
    }
}
```

## SOLID

- **Single Responsibility (SRP).** One reason to modify a class. Extract I/O,
  validation, parsing, and persistence into separate collaborators.
- **Open/Closed (OCP).** Prefer interfaces and polymorphism over growing
  `switch` statements and `if/else` ladders.
- **Liskov Substitution (LSP).** A subtype must be fully substitutable for its
  base type. Never throw `UnsupportedOperationException` from an overridden
  method.
- **Interface Segregation (ISP).** Small, focused interfaces. Clients must not
  depend on methods they do not use.
- **Dependency Inversion (DIP).** High-level and low-level modules depend on
  abstractions. In Plain Java, use strict constructor injection.

```java
// Prefer: constructor injection of an abstraction
public final class ScoreboardService implements Scoreboard {

    private final Clock clock;
    private final Map<UUID, Match> matches = new ConcurrentHashMap<>();

    public ScoreboardService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }
}
```

## Composition over inheritance

Reuse behavior by delegating to collaborators, not by extending a class:

```java
// Avoid: inheritance for reuse
public class TimedScoreboard extends ScoreboardService { ... }

// Prefer: composition
public final class TimedScoreboard implements Scoreboard {

    private final Scoreboard delegate;
    private final Clock clock;

    public TimedScoreboard(Scoreboard delegate, Clock clock) {
        this.delegate = delegate;
        this.clock = clock;
    }

    @Override
    public Match startMatch(String homeTeam, String awayTeam) {
        return delegate.startMatch(homeTeam, awayTeam);
    }
}
```

## Encapsulation and immutability

- Keep data and behavior together.
- Mark fields `private final` whenever possible.
- Use native `record` for pure data (Value Objects, DTOs).
- Implement a static inner `Builder` only when constructing a genuinely
  complex object; do not add builders speculatively (YAGNI).
- Never expose mutable internal collections. Return defensive copies or
  unmodifiable views.

```java
// Prefer: immutable value object
public record Match(UUID id, String homeTeam, String awayTeam,
                    int homeScore, int awayScore, MatchStatus status,
                    Instant startedAt) { }
```

## File structure

- **One top-level type per file.** Every record, enum, class, and interface
  lives in its own file named after the type.
- **No nested types.** Do not hide a helper record or class inside the type
  that uses it. Extract it to a package-private top-level type in the same
  package.
- **Visibility follows the API.** Make the extracted type `public` only when it
  is part of the published contract; otherwise keep it package-private so it
  cannot leak out of the package.

```java
// Avoid: helper record buried inside the service
public final class ScoreboardService implements Scoreboard {

    private record TrackedMatch(Match match, long startSequence) { }
}
```

```java
// Prefer: package-private top-level type in TrackedMatch.java
record TrackedMatch(Match match, long startSequence) {

    TrackedMatch withScore(int homeScore, int awayScore) {
        return new TrackedMatch(match.withScore(homeScore, awayScore), startSequence);
    }
}
```

## Magic literals

- **No magic strings or numbers.** Give every repeated or meaningful literal a
  `private static final` constant with an intention-revealing name, for example
  `MATCH_NOT_FOUND_MESSAGE` or `MAX_RETRIES`.
- **Error messages are literals too.** Keep the whole template in a constant
  and fill it with `.formatted(...)`; do not concatenate fragments at the throw
  site.
- **Do not hoist test data.** Values such as `"Mexico"` or `10` in a test's
  Arrange section document the scenario and stay local; only configuration and
  message literals belong in constants.

```java
// Avoid: magic string and inline concatenation
throw new NoSuchElementException("Match not found: " + matchId);
```

```java
// Prefer: constant template filled at the throw site
private static final String MATCH_NOT_FOUND_MESSAGE = "Match not found: %s";

throw new NoSuchElementException(MATCH_NOT_FOUND_MESSAGE.formatted(matchId));
```

## Naming and comments

- **Code is documentation.** Prefer clear class, method, and variable names
  over explanatory comments.
- **A comment is a failure** when it explains what the code does. Extract the
  block into a well-named private method instead.
- Comments are acceptable only for non-obvious constraints, external formats,
  or genuinely complex algorithms.
- **No commented-out code. No dead code. No TODO graveyards.**

## Error handling

- Do not return `null`; it hides intent and produces `NullPointerException`.
- Use `Optional<T>` as a return type for queries that may have no result:

```java
// Prefer: Optional return type
Optional<Match> findById(UUID id);

// Avoid: Optional parameter
void update(UUID id, Optional<Match> match);
```

- Prefer unchecked exceptions (`IllegalArgumentException`,
  `IllegalStateException`, `NoSuchElementException`) over checked exceptions
  that pollute higher-layer signatures.
- Validate at the boundary and fail fast with a precise message.

## Null safety

- **Guard reference parameters at the boundary.** Reject `null` with an
  `IllegalArgumentException` and a precise message, or model absence with
  `Optional` as a return type. A method must never throw `NullPointerException`
  for input it could have validated.
- **Put the literal first in string comparisons.** `"text".equals(value)` is
  safe when `value` is `null`; `value.equals("text")` is not.
- **Compare enums with `==`.** It is null-safe and never invokes a method on a
  `null` reference.
- **Use `Objects.equals(a, b)`** when both sides are variables that may be
  `null`.
- **Use `Objects.requireNonNull(..., message)` only for parameters of public
  constructors and factories**, where `null` can come from outside. It fails
  fast at the source with a clear message; it does not hide the error.
- **Do not guard what cannot be null.** Internal and package-private types
  built only from already validated values need no `requireNonNull`; extra
  guards are dead code.
- **Guard before instance calls.** After a null guard,
  `value.equalsIgnoreCase(other)` is safe; without it, guard both sides first.
- **Never unbox a nullable wrapper implicitly.** Check for `null` before
  assigning an `Integer`, `Long`, or `Boolean` to a primitive.

```java
// Avoid: NPE when value is null
if (value.equals("active")) { ... }

// Prefer: literal-first comparison
if ("active".equals(value)) { ... }

// Avoid: NPE when either team is null
if (homeTeam.equalsIgnoreCase(awayTeam)) { ... }

// Prefer: guard both sides, then compare
requireTeamName(homeTeam);
requireTeamName(awayTeam);
if (homeTeam.equalsIgnoreCase(awayTeam)) { ... }
```

## Concurrency (when state is shared)

- Prefer immutable values plus thread-safe collections
  (`ConcurrentHashMap`, `AtomicLong`, `LongAdder`).
- A concurrent collection does not make compound operations atomic. Use
  `compute`, `computeIfPresent`, or `merge` for check-then-act logic.
- Avoid a global `synchronized` bottleneck when independent per-key updates
  are enough; document the chosen trade-off.
- Never publish partially constructed objects; make fields `final`.

## Tests

- Use **JUnit 5** and **AssertJ** (`assertThat(...)`).
- Structure every test with `// Arrange`, `// Act`, `// Assert`.
- Name tests as behavior: `shouldReturnMatchesOrderedByTotalScoreThenStartTime`.
- One logical assertion per test; assert observable behavior, not internals.
- Cover the happy path, validation, lifecycle errors, ordering ties, and
  concurrency where relevant.
- No `Thread.sleep` in tests. Inject a `Clock` or use deterministic
  synchronization primitives.

```java
@Test
void shouldOrderMatchesByTotalScoreThenStartTime() {
    // Arrange
    Scoreboard scoreboard = new ScoreboardService(Clock.systemUTC());
    Match lowScore = scoreboard.startMatch("Germany", "France");
    Match highScore = scoreboard.startMatch("Spain", "Brazil");
    scoreboard.updateScore(highScore.id(), 10, 2);

    // Act
    List<Match> summary = scoreboard.getSummary();

    // Assert
    assertThat(summary)
            .extracting(Match::id)
            .containsExactly(highScore.id(), lowScore.id());
}
```

## Review checklist

Before considering Java code done, verify:

- [ ] Only native Java mechanisms; no code-generating annotations.
- [ ] SOLID respected; no god classes.
- [ ] No arrow anti-pattern; guard clauses used.
- [ ] Methods small, one level of abstraction, at most 2–3 arguments.
- [ ] Composition used instead of inheritance for reuse.
- [ ] Fields `private final` whenever possible.
- [ ] No `final` on method parameters.
- [ ] No `Optional` in method parameters; `Optional` only as a return type.
- [ ] No `null` returns; unchecked exceptions with clear messages.
- [ ] One top-level type per file; no nested records or helper classes.
- [ ] Extracted helper types are package-private unless they are public API.
- [ ] No magic strings or numbers; meaningful literals are `private static final`.
- [ ] Error messages are constants filled with `.formatted(...)`.
- [ ] Reference parameters are guarded; no method NPEs on `null` input.
- [ ] String comparisons are literal-first (`"text".equals(value)`).
- [ ] Public constructors and factories reject `null` parameters with `Objects.requireNonNull`; no redundant guards on internal types.
- [ ] YAGNI respected; no speculative abstractions.
- [ ] No commented-out code or dead code.
- [ ] Tests use Arrange/Act/Assert and AssertJ.
- [ ] Boy Scout Rule: the code is cleaner than when you found it.
