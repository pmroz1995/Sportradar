# Live Football World Cup Scoreboard

A small Java 17 library for managing multiple simultaneous football matches. It is an in-memory component with no runtime dependencies.

## Requirements covered

- Start a match with a zero-zero score.
- Update the score of an in-progress match.
- Finish a match.
- Return an immutable summary of in-progress matches ordered by total score (descending), then start time (newest first).
- One additional operation: retrieve a match by its UUID, including after it has finished.

## Build and test

Requires JDK 17+ and Maven 3.8+.

```bash
mvn clean verify
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

- **Small public API:** `Scoreboard` defines the five supported operations; `ScoreboardService` is the in-memory implementation.
- **Immutable snapshots:** `Match` is a Java record. Callers cannot mutate internal state through returned objects.
- **Identity:** Every match receives a UUID, avoiding ambiguity when teams participate in more than one match.
- **Lifecycle:** A match starts at `0–0`, can be updated while in progress, and can be finished once. Finished matches remain available through `getMatchById`, but are excluded from the live summary.
- **Ordering:** Summary order is total score descending, then start instant descending. A monotonically increasing internal start sequence resolves identical timestamps deterministically (for example, when tests use a fixed clock).
- **Concurrency:** Public service methods are synchronized. This is intentionally straightforward and provides atomic state transitions for this small in-memory implementation.
- **Time:** `Clock` is injected, with UTC system time as the default. This makes time-dependent behavior deterministic in tests.

## Assumptions and validation

- Team names must be non-null and non-blank; leading/trailing whitespace is trimmed.
- A team cannot play itself (case-insensitive comparison).
- Different matches may contain the same team; the library does not enforce tournament scheduling rules.
- Scores must be non-negative. Score corrections, including decreasing a score, are allowed because upstream sports feeds may correct erroneous events.
- Updating or finishing an unknown match throws `NoSuchElementException`.
- Updating or finishing an already finished match throws `IllegalStateException`.
- Invalid names, negative scores, and null identifiers are rejected.
- The summary is a snapshot: callers receive an unmodifiable list.

## Additional feature: lookup by match ID

`getMatchById(UUID)` returns an `Optional<Match>` for both live and finished matches. This was chosen because consumers commonly need to reconcile an event or query a specific match without scanning the live summary. Keeping finished matches in memory also makes the lifecycle observable.

## Trade-offs and future considerations

- **In-memory only:** State is lost when the process stops. Persistence and distributed consistency are outside this exercise's scope.
- **Synchronized methods:** Easy to reason about and correct for modest workloads; a read/write lock or concurrent data model could improve throughput if profiling justified it.
- **UUID identifiers:** Convenient for independent callers and distributed systems, but less human-friendly than a domain-specific match number.
- **No event history:** Only the latest score is stored. An audit/event stream would be appropriate for replay and correction workflows but is not required here.
- **No deletion operation:** The public API intentionally stays limited to the required four operations plus exactly one additional operation.

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
    Scoreboard.java
    ScoreboardService.java
  test/java/com/sportradar/scoreboard/
    ScoreboardServiceTest.java
README.md
AI.md
PROMPT.md
pom.xml
```
