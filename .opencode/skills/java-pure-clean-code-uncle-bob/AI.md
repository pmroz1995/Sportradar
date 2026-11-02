# AI usage

## Summary

AI assistants were used throughout the exercise, through the OpenCode coding agent:

- **Requirements analysis:** the agent read the exercise, compared it with the existing repository, and helped separate the business requirements from the technical scaffold.
- **Guidelines as an artifact:** the agent wrote a reusable clean code skill from rules I dictated; it was then used to steer and review the implementation.
- **Implementation:** Claude Sonnet 5.5 implemented the library, tests, documentation and git history from the agreed plan.

The business scope, the rules and the choice of additional feature were mine; the AI proposed options and wrote the code. Every change was checked by running `mvnw clean verify` (51 tests) and by a review against the skill's checklist.

## Business decision path

The order below is the order in which the business rules of the solution took shape. "AI" marks an option proposed by the agent; the decision was mine.

1. **Product scope.** The exercise is a library that manages live matches in memory. There is no HTTP layer and no persistence; the state lives as long as the process.
2. **Match lifecycle.** A match starts at 0–0 and stays live until it is finished. Finishing removes it from the live summary but keeps the final result available for later lookup, so the result is not lost when the match ends.
3. **Identity.** Every match gets its own generated id. AI proposed a UUID instead of the pair of team names, because the same teams can meet more than once.
4. **Team rules.** Both names are required and trimmed. A team cannot play against itself, compared case-insensitively. Beyond that the library does not enforce tournament scheduling, so the same team may appear in different matches.
5. **Score rules.** Scores are non-negative. An update replaces the current score, and lowering it is allowed because upstream feeds correct erroneous events. AI raised the delta-versus-replacement question; replacement was my choice.
6. **Summary.** Only live matches are listed, ordered by total score descending and then by most recently started. AI noted that two matches with the same start instant would make the order ambiguous, so the start order breaks the tie. The summary is a snapshot: later changes do not alter a list already returned.
7. **Additional operation.** From the options AI listed I chose lookup by match id. The summary only exposes live matches, but a consumer needs final results and a way to reconcile a single event from an upstream feed. Finished matches are already retained, so the feature adds no new data, and an `Optional` return makes "not found" explicit.
8. **Errors as business feedback.** Invalid input, an unknown match and an already finished match are reported as distinct outcomes, so a caller can tell a bad request from a stale one. A rejected call never changes the scoreboard.
9. **Missing data is invalid data.** A missing team name or match id is rejected before anything changes, instead of failing later or being treated as an empty value. Name comparisons are written so a missing value cannot crash the library; the same applies to internal collaborators such as the clock.

## Prompt history

Prompts are paraphrased in chronological order; only the substance is kept.

1. *Read `README.md` and `PROMPT.md`, but do nothing yet.* → Both files were summarised, nothing was changed.
2. *Will there be a REST API, and where is the data stored?* → The exercise asks for a library, so no REST API; data lives in memory. The agent noticed that the repository was a Spring Boot scaffold, which contradicted the README ("no runtime dependencies").
3. *Use concurrency-safe data structures such as `ConcurrentHashMap`.* → Agent explained that a concurrent map alone does not make check-then-act operations atomic, and proposed `compute` / `computeIfPresent`.
4. *Which is better, `synchronized` or `ConcurrentHashMap`?* → Trade-off discussion: `synchronized` is simpler and gives a consistent snapshot, `ConcurrentHashMap` scales better and avoids a global lock.
5. *Here is the original exercise text.* (pasted in full) → Agent compared it with the repository and asked for decisions. Outcome:
   - plain Maven library instead of Spring Boot;
   - Java 17, because the exercise does not specify a version;
   - `ConcurrentHashMap` with atomic `compute`.
6. *Create a skill for clean code:* SOLID, YAGNI, guard clauses instead of the arrow anti-pattern, composition over inheritance, tests in `// Arrange`, `// Act`, `// Assert` style with AssertJ, `final` fields, no `final` parameters, no `Optional` parameters, plus a pasted set of Uncle Bob rules; in English. → `.opencode/skills/java-pure-clean-code-uncle-bob/SKILL.md`.
7. *Add the agreed decisions to the prompt, in English, and add the skill to the directory tree.* → "Agreed implementation decisions" section in `PROMPT.md`; tree updated in `README.md`.
8. *Remove the mentions of Spring Boot and Lombok from the skill; this task does not concern them.* → Skill edited.
9. *Implement the prompt using the skill.* → Implementation by Claude Sonnet 5.5 (this commit history).

## Contextual information given to the AI

- The original exercise text (kept as `PROMPT.md` next to this file, with the agreed decisions appended).
- The decisions: plain Maven library, Java 17, `ConcurrentHashMap` + atomic `compute`, `getMatchById` as the single additional operation, a separate commit for it, `README.md` and `AI.md` as deliverables.
- The clean code skill (below).

## Artifacts that guided the implementation

| Artifact                                                    | Role                                                                                              |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `.opencode/skills/java-pure-clean-code-uncle-bob/PROMPT.md` | The exercise requirements and the agreed implementation decisions.                                |
| `.opencode/skills/java-pure-clean-code-uncle-bob/SKILL.md`  | Coding and testing rules the implementation was written and reviewed against.                     |
| `README.md`                                                 | Design reasoning, assumptions, trade-offs and the additional feature, written alongside the code. |

## How the output was validated

- The acceptance scenario from the exercise is a test (`shouldOrderAcceptanceScenarioByTotalScoreThenMostRecentlyStarted`).
- Tests cover validation, lifecycle errors, ordering ties (identical and different start instants), snapshot behaviour, and concurrent start, finish and update.
- Concurrency tests use a start gate (`CountDownLatch`) instead of sleeps, so they are deterministic in setup.
- The skill's review checklist was applied manually to the final code: no `final` parameters, no `Optional` parameters, guard clauses instead of nested conditionals, `final` fields, composition over inheritance, and Arrange/Act/Assert with AssertJ.

## Things I would still question in review

- A concurrent summary is weakly consistent across matches (documented in the README trade-offs).
- `Scoreboard` has a single implementation. It is kept as the public contract of the library rather than for speculative extension.
- Finished matches are never evicted from memory.
