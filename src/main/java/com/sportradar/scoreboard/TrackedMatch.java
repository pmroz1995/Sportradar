package com.sportradar.scoreboard;

import java.time.Instant;

record TrackedMatch(Match match, long startSequence) {

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
