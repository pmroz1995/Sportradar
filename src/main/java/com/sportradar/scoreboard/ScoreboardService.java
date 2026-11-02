package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ScoreboardService implements Scoreboard {

    private static final String CLOCK_NULL_MESSAGE = "clock must not be null";
    private static final String MATCH_NOT_FOUND_MESSAGE = "Match not found: %s";

    private static final Comparator<TrackedMatch> SUMMARY_ORDER = Comparator
            .comparingLong(TrackedMatch::totalScore)
            .thenComparing(TrackedMatch::startedAt)
            .thenComparingLong(TrackedMatch::startSequence)
            .reversed();

    private final Clock clock;
    private final Map<UUID, TrackedMatch> matches = new ConcurrentHashMap<>();
    private final AtomicLong startSequence = new AtomicLong();

    public ScoreboardService() {
        this(Clock.systemUTC());
    }

    public ScoreboardService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, CLOCK_NULL_MESSAGE);
    }

    @Override
    public Match startMatch(String homeTeam, String awayTeam) {
        String home = MatchValidator.normalizedTeamName(homeTeam);
        String away = MatchValidator.normalizedTeamName(awayTeam);
        MatchValidator.requireDistinctTeams(home, away);

        Match match = Match.started(UUID.randomUUID(), home, away, clock.instant());
        matches.put(match.id(), new TrackedMatch(match, startSequence.incrementAndGet()));
        return match;
    }

    @Override
    public void updateScore(UUID matchId, int homeScore, int awayScore) {
        MatchValidator.requireMatchId(matchId);
        MatchValidator.requireNonNegative(homeScore, awayScore);

        TrackedMatch updated = matches.computeIfPresent(
                matchId, (id, tracked) -> tracked.withScore(homeScore, awayScore));
        requireFound(updated, matchId);
    }

    @Override
    public void finishMatch(UUID matchId) {
        MatchValidator.requireMatchId(matchId);

        TrackedMatch finished = matches.computeIfPresent(matchId, (id, tracked) -> tracked.finished());
        requireFound(finished, matchId);
    }

    @Override
    public List<Match> getSummary() {
        return matches.values().stream()
                .filter(TrackedMatch::isInProgress)
                .sorted(SUMMARY_ORDER)
                .map(TrackedMatch::match)
                .toList();
    }

    @Override
    public Optional<Match> getMatchById(UUID matchId) {
        MatchValidator.requireMatchId(matchId);

        return Optional.ofNullable(matches.get(matchId)).map(TrackedMatch::match);
    }

    private static void requireFound(TrackedMatch tracked, UUID matchId) {
        if (tracked == null) {
            throw new NoSuchElementException(MATCH_NOT_FOUND_MESSAGE.formatted(matchId));
        }
    }
}
