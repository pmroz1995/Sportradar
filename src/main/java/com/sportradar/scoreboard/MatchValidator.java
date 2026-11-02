package com.sportradar.scoreboard;

import java.util.UUID;

final class MatchValidator {

    private MatchValidator() {
    }

    static String normalizedTeamName(String teamName) {
        if (teamName == null) {
            throw new IllegalArgumentException("Team name must not be null");
        }
        String trimmed = teamName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }
        return trimmed;
    }

    static void requireDistinctTeams(String homeTeam, String awayTeam) {
        if (homeTeam.equalsIgnoreCase(awayTeam)) {
            throw new IllegalArgumentException("A team cannot play against itself: " + homeTeam);
        }
    }

    static void requireNonNegative(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException(
                    "Scores must not be negative: " + homeScore + " - " + awayScore);
        }
    }

    static void requireMatchId(UUID matchId) {
        if (matchId == null) {
            throw new IllegalArgumentException("Match id must not be null");
        }
    }
}
