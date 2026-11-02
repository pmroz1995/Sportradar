package com.sportradar.scoreboard;

import java.util.UUID;

final class MatchValidator {

    private static final String TEAM_NAME_NULL_MESSAGE = "Team name must not be null";
    private static final String TEAM_NAME_BLANK_MESSAGE = "Team name must not be blank";
    private static final String SAME_TEAM_MESSAGE = "A team cannot play against itself: %s";
    private static final String NEGATIVE_SCORE_MESSAGE = "Scores must not be negative: %d - %d";
    private static final String MATCH_ID_NULL_MESSAGE = "Match id must not be null";

    private MatchValidator() {
    }

    static String normalizedTeamName(String teamName) {
        requireTeamName(teamName);
        String trimmed = teamName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(TEAM_NAME_BLANK_MESSAGE);
        }
        return trimmed;
    }

    static void requireDistinctTeams(String homeTeam, String awayTeam) {
        requireTeamName(homeTeam);
        requireTeamName(awayTeam);

        if (homeTeam.equalsIgnoreCase(awayTeam)) {
            throw new IllegalArgumentException(SAME_TEAM_MESSAGE.formatted(homeTeam));
        }
    }

    static void requireNonNegative(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException(NEGATIVE_SCORE_MESSAGE.formatted(homeScore, awayScore));
        }
    }

    static void requireMatchId(UUID matchId) {
        if (matchId == null) {
            throw new IllegalArgumentException(MATCH_ID_NULL_MESSAGE);
        }
    }

    private static void requireTeamName(String teamName) {
        if (teamName == null) {
            throw new IllegalArgumentException(TEAM_NAME_NULL_MESSAGE);
        }
    }
}
