# Live Football World Cup Scoreboard

A small Java 17 library that manages multiple simultaneous football matches. It is an in-memory component with no runtime dependencies, built as a plain Maven project.

## Requirements covered

Core operations:

1. **Start a new match** with a 0–0 score.
2. **Update the score** of a match in progress.
3. **Finish a match** in progress.
4. **Get a summary of matches in progress**, ordered by total score (descending) and, for equal totals, by most recently started first.

Additional operation (one, chosen by me):

5. **Get a match by its id**, including after it has finished. See [Additional feature](#additional-feature-lookup-by-match-id).

### Example scenario

Matches are started and updated in this order:

| Order started | Match | Score |
| --- | --- | --- |
| 1 | Mexico – Canada | 0 – 5 |
| 2 | Spain – Brazil | 10 – 2 |
| 3 | Germany – France | 2 – 2 |
| 4 | Uruguay – Italy | 6 – 6 |
| 5 | Argentina – Australia | 3 – 1 |

The summary is:

1. Uruguay 6 – Italy 6
2. Spain 10 – Brazil 2
3. Mexico 0 – Canada 5
4. Argentina 3 – Australia 1
5. Germany 2 – France 2

This scenario is an automated test (`shouldOrderAcceptanceScenarioByTotalScoreThenMostRecentlyStarted`).

## Build and test

Requires JDK 17 or newer. The Maven wrapper is included, so a local Maven installation is optional.

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

## Assumptions

The requirements leave several questions open. These are the answers I chose:

- **Team names** must be non-null and non-blank; leading and trailing whitespace is trimmed.
- **A team cannot play itself**; the comparison is case-insensitive.
- **The same team may appear in different matches.** The library does not enforce tournament scheduling rules.
- **Scores** must be non-negative. `updateScore` replaces the current score; it is not a delta.
- **Score corrections are allowed**, including lowering a score, because upstream sports feeds may correct erroneous events.
- **Match identity** is a generated UUID, so a pair of teams is not a key.
- **Finishing** a match removes it from the summary.
- **Errors:**
  - Invalid names, negative scores, and null identifiers throw `IllegalArgumentException`.
  - Updating or finishing an unknown match throws `NoSuchElementException`.
  - Updating or finishing an already finished match throws `IllegalStateException`.
  - A rejected call never changes the scoreboard.
- **The summary is a snapshot:** callers receive an unmodifiable list that does not change when the scoreboard changes afterwards.

## Design and reasoning

- **Small public API:** `Scoreboard` defines the supported operations; `ScoreboardService` is the in-memory implementation.
- **Immutable snapshots:** `Match` is a Java record. State transitions (`withScore`, `finished`) are package-private, so callers cannot change a match through a returned object and only the service can move it through its lifecycle.
- **Lifecycle:** A match starts at 0–0, can be updated while in progress, and can be finished once. Finished matches stay in memory with status `FINISHED`, so a repeated update or finish is reported as a lifecycle error (`IllegalStateException`) instead of being confused with an unknown id.
- **Ordering:** Summary order is total score descending, then start instant descending. A monotonically increasing internal start sequence resolves identical timestamps deterministically (for example, when a fixed clock is used in tests). The total is a `long`, so very large scores cannot overflow and corrupt the order.
- **Concurrency:** State lives in a `ConcurrentHashMap`. Check-then-act transitions (update, finish) run inside `computeIfPresent`, which is atomic per match, so two threads can never finish the same match twice. The start sequence is an `AtomicLong`. There is no global lock, so independent matches never block each other and readers never block writers.
- **Time:** `Clock` is injected, with UTC system time as the default, which keeps time-dependent behaviour deterministic in tests.
- **Code style:** The code follows the repository's clean code skill ([`SKILL.md`](.opencode/skills/java-pure-clean-code-uncle-bob/SKILL.md)): SOLID, YAGNI, guard clauses instead of nested conditionals, composition over inheritance, `final` fields, and tests written as Arrange / Act / Assert with AssertJ.

## Additional feature: lookup by match ID

`Optional<Match> getMatchById(UUID)` returns a match whether it is in progress or already finished, and an empty `Optional` for an unknown id. A null id is rejected with `IllegalArgumentException`.

**Why this feature:** the summary only lists live matches, so a consumer that needs the final result of a match, or wants to reconcile a single event from an upstream feed, would otherwise have to track every `Match` it ever received. Looking a match up by id is the most common follow-up to `startMatch`, which hands out the id. It is also cheap and safe here: finished matches are already retained for lifecycle checks, so the feature adds no new state, only a read-only view of it. An `Optional` return value makes "not found" explicit without exposing `null` or throwing for a normal query.

It was introduced in its own git commit.

## Trade-offs and future considerations

- **In-memory only:** State is lost when the process stops. Persistence and distributed consistency are outside this exercise's scope.
- **Per-match atomicity, not a global snapshot:** `ConcurrentHashMap` iteration is weakly consistent. A summary built while several matches are being updated reflects each match at a single point in time, but not necessarily all matches at the same instant. A coarse `synchronized` implementation would give a fully consistent snapshot at the cost of serialising every operation; this implementation favours throughput and independent updates.
- **Finished matches are never evicted:** Memory grows with the number of matches played. An eviction policy would be needed for a long-running process; it is not required here.
- **UUID identifiers:** Convenient for independent callers and distributed systems, but less human-friendly than a domain-specific match number.
- **No event history:** Only the latest score is stored. An audit or event stream would be appropriate for replay and correction workflows but is not required here.
- **Single implementation behind `Scoreboard`:** The interface is kept as the library's public contract, not for speculative extension.
- **No deletion operation:** The public API intentionally stays limited to the four required operations plus exactly one additional operation.

## AI usage

The AI usage summary, prompt history, and the artifacts that guided the implementation are in [`AI.md`](.opencode/skills/java-pure-clean-code-uncle-bob/AI.md). The exercise requirements and the decisions agreed before coding are in [`PROMPT.md`](.opencode/skills/java-pure-clean-code-uncle-bob/PROMPT.md). Both sit next to the clean code skill that guided the implementation.

## Project structure

```text
.opencode/
  skills/
    java-pure-clean-code-uncle-bob/
      AI.md          AI usage summary, prompt history and guiding artifacts
      PROMPT.md      Exercise requirements and agreed implementation decisions
      SKILL.md       Clean code skill the implementation was written against
.mvn/wrapper/        Maven wrapper configuration
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
mvnw
mvnw.cmd
pom.xml
README.md
```
