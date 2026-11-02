# Live Football World Cup Scoreboard

A small Java 17 library for managing multiple simultaneous football matches. It is an in-memory component with no runtime dependencies.

## Requirements covered

- Start a match with a zero-zero score.
- Update the score of an in-progress match.
- Finish a match.
- Return an immutable summary of in-progress matches ordered by total score (descending), then start time (newest first).
- One additional operation: retrieve a match by its UUID, including after it has finished.

## Build and test

Requires JDK 17+. The Maven wrapper is included, so a local Maven installation is optional.

```bash
./mvnw clean verify      # Linux / macOS
mvnw.cmd clean verify    # Windows
```

## Quick example

```java
Scoreboard scoreboard = new ScoreboardService();

Match match = scoreboard.startMatch("Mexico", "Canada");
scoreboard.updateScore(match.id(), 0, 5);

List<Match> liveMatches = scoreboard.getSummary();
scoreboard.finishMatch(match.id());

Optional<Match> historicalMatch = scoreboard.getMatchById(match.id());
```

## Design and reasoning

- **Small public API:** `Scoreboard` defines the supported operations; `ScoreboardService` is the in-memory implementation.
- **Immutable snapshots:** `Match` is a Java record. State transitions (`withScore`, `finished`) are package-private, so callers cannot change a match through a returned object and only the service can move it through its lifecycle.
- **Identity:** Every match receives a UUID, avoiding ambiguity when a team takes part in more than one match.
- **Lifecycle:** A match starts at `0–0`, can be updated while in progress, and can be finished once. Finished matches stay in memory with status `FINISHED` so that a repeated update or finish is reported as a lifecycle error (`IllegalStateException`) instead of being confused with an unknown id. They are excluded from the summary.
- **Ordering:** Summary order is total score descending, then start instant descending. A monotonically increasing internal start sequence resolves identical timestamps deterministically (for example, when a fixed clock is used in tests). The total is computed as a `long`, so very large scores cannot overflow and corrupt the order.
- **Concurrency:** State lives in a `ConcurrentHashMap`. Check-then-act transitions (update, finish) run inside `computeIfPresent`, which is atomic per match, so two threads can never finish the same match twice or tear a score. The start sequence is an `AtomicLong`. There is no global lock, so independent matches never block each other and readers never block writers.
- **Time:** `Clock` is injected, with UTC system time as the default, which keeps time-dependent behaviour deterministic in tests.
- **Code style:** The code follows the repository's clean code skill (`.opencode/skills/java-pure-clean-code-uncle-bob`): SOLID, YAGNI, guard clauses instead of nested conditionals, composition over inheritance, `final` fields, and tests written as Arrange / Act / Assert with AssertJ.

## Assumptions and validation

- Team names must be non-null and non-blank; leading and trailing whitespace is trimmed.
- A team cannot play itself (case-insensitive comparison).
- Different matches may contain the same team; the library does not enforce tournament scheduling rules.
- Scores must be non-negative. Score corrections, including decreasing a score, are allowed because upstream sports feeds may correct erroneous events.
- The score passed to `updateScore` replaces the current score; it is not a delta.
- Updating or finishing an unknown match throws `NoSuchElementException`.
- Updating or finishing an already finished match throws `IllegalStateException`.
- Invalid names, negative scores, and null identifiers throw `IllegalArgumentException`. A rejected call never changes the scoreboard.
- The summary is a snapshot: callers receive an unmodifiable list that does not change when the scoreboard changes afterwards.

## Additional feature: lookup by match ID

`Optional<Match> getMatchById(UUID)` returns a match whether it is in progress or already finished, and an empty `Optional` for an unknown id. A null id is rejected with `IllegalArgumentException`.

**Why this feature:** the summary only lists live matches, so a consumer that needs the final result of a match, or wants to reconcile a single event from an upstream feed, would otherwise have to track every `Match` it ever received. Looking a match up by id is the most common follow-up to `startMatch`, which hands out the id. It is also cheap and safe here: finished matches are already retained for lifecycle checks, so the feature adds no new state, only a read-only view of it. An `Optional` return value makes "not found" explicit without exposing `null` or throwing for a normal query.

## Trade-offs and future considerations

- **In-memory only:** State is lost when the process stops. Persistence and distributed consistency are outside this exercise's scope.
- **Per-match atomicity, not a global snapshot:** `ConcurrentHashMap` iteration is weakly consistent. A summary built while several matches are being updated reflects each match at a single point in time, but not necessarily all matches at the same instant. A coarse `synchronized` implementation would give a fully consistent snapshot at the cost of serialising every operation; this implementation favours throughput and simplicity of independent updates.
- **Finished matches are never evicted:** Memory grows with the number of matches played. An eviction policy would be needed for a long-running process; it is not required here.
- **UUID identifiers:** Convenient for independent callers and distributed systems, but less human-friendly than a domain-specific match number.
- **No event history:** Only the latest score is stored. An audit or event stream would be appropriate for replay and correction workflows but is not required here.
- **No deletion operation:** The public API intentionally stays limited to the required operations plus exactly one additional operation.

## Project structure

```text
.opencode/
  skills/
    java-pure-clean-code-uncle-bob/
      SKILL.md
src/
  main/java/com/sportradar/scoreboard/
    Match.java
    MatchStatus.java
    MatchValidator.java
    Scoreboard.java
    ScoreboardService.java
  test/java/com/sportradar/scoreboard/
    MatchTest.java
    MutableClock.java
    ScoreboardServiceConcurrencyTest.java
    ScoreboardServiceTest.java
README.md
PROMPT.md
pom.xml
```
