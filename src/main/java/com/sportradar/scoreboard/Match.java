package com.sportradar.scoreboard;

import java.time.Instant;
import java.util.UUID;

public record Match(
        UUID id,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        MatchStatus status,
        Instant startedAt) {

    private static final String ALREADY_FINISHED_MESSAGE = "Match %s has already finished";

    static Match started(UUID id, String homeTeam, String awayTeam, Instant startedAt) {
        return new Match(id, homeTeam, awayTeam, 0, 0, MatchStatus.IN_PROGRESS, startedAt);
    }

    public long totalScore() {
        return (long) homeScore + awayScore;
    }

    public boolean isInProgress() {
        return status == MatchStatus.IN_PROGRESS;
    }

    Match withScore(int newHomeScore, int newAwayScore) {
        requireInProgress();
        return new Match(id, homeTeam, awayTeam, newHomeScore, newAwayScore, status, startedAt);
    }

    Match finished() {
        requireInProgress();
        return new Match(id, homeTeam, awayTeam, homeScore, awayScore, MatchStatus.FINISHED, startedAt);
    }

    private void requireInProgress() {
        if (!isInProgress()) {
            throw new IllegalStateException(ALREADY_FINISHED_MESSAGE.formatted(id));
        }
    }
}
