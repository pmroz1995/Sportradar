# Prompt – Live Football World Cup Scoreboard coding exercise

You are asked to implement a simple library that manages a Live Football World Cup Scoreboard.

## Goal

Create a review-ready Git repository containing a simple Java package in a Maven project.

## Required operations

1. Start a new match.
2. Update the score.
3. Finish a match.
4. Get a summary of matches in progress, ordered by:
   - Total score (home score + away score), descending.
   - If tied, most recently started match first.
5. Add exactly one additional operation of your choice and explain the feature in `README.md`.

The library must support multiple simultaneous matches.

## Example acceptance scenario

Start and update the following matches in this order:

- Mexico 0 – Canada 5
- Spain 10 – Brazil 2
- Germany 2 – France 2
- Uruguay 6 – Italy 6
- Argentina 3 – Australia 1

The expected summary order is:

1. Uruguay 6 – Italy 6
2. Spain 10 – Brazil 2
3. Mexico 0 – Canada 5
4. Argentina 3 – Australia 1
5. Germany 2 – France 2

## Deliverables

- Library implementation.
- Maven project configuration.
- Automated tests.
- `README.md` documenting assumptions, reasoning, the additional feature, and trade-offs.
- `AI.md` containing a short summary of AI use, prompt history, contextual information, and artifacts that guided implementation.
- A Git repository suitable for code review.
- A distinct Git commit introducing the additional feature.

## Engineering expectations

Use clear APIs, sensible validation, maintainable code, meaningful tests, and document design choices. The requirements intentionally leave some choices open; state assumptions explicitly rather than silently relying on them.

## Agreed implementation decisions

The following decisions were agreed before implementation:

- **Project type:** a plain Maven library (JAR), without Spring Boot and
  without Lombok.
- **Java version:** Java 17 (LTS).
- **Concurrency:** thread-safe state based on `ConcurrentHashMap` with atomic
  `compute` / `computeIfPresent` operations; no global `synchronized` lock.
- **Additional operation:** `getMatchById(UUID)`, returning both live and
  finished matches as `Optional<Match>`.
- **Documentation:** `README.md` (assumptions, reasoning, trade-offs, and the
  additional feature) and `AI.md` (AI usage summary, prompt history, and
  contextual information).
- **Git history:** a distinct commit that introduces the additional feature.
- **Code style:** Plain Java 17+ following the
  `java-pure-clean-code-uncle-bob` skill (SOLID, YAGNI, guard clauses,
  composition over inheritance, and AssertJ tests in Arrange/Act/Assert style).
