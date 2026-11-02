package com.sportradar.scoreboard;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ScoreboardService implements Scoreboard {

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
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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

    private static void requireFound(TrackedMatch tracked, UUID matchId) {
        if (tracked == null) {
            throw new NoSuchElementException("Match not found: " + matchId);
        }
    }

    private record TrackedMatch(Match match, long startSequence) {

        long totalScore() {
            return match.totalScore();
        }

        Instant startedAt() {
            return match.startedAt();
        }

        boolean isInProgress() {
            return match.isInProgress();
        }

        TrackedMatch withScore(int homeScore, int awayScore) {
            return new TrackedMatch(match.withScore(homeScore, awayScore), startSequence);
        }

        TrackedMatch finished() {
            return new TrackedMatch(match.finished(), startSequence);
        }
    }
}
