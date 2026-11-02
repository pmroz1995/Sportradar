package com.sportradar.scoreboard;

import java.util.List;
import java.util.UUID;

public interface Scoreboard {

    /**
     * Starts a match with a 0-0 score.
     *
     * @throws IllegalArgumentException if a team name is null or blank, or both teams are the same
     */
    Match startMatch(String homeTeam, String awayTeam);

    /**
     * Replaces the score of a match in progress. Lower scores are accepted so that feed corrections can be applied.
     *
     * @throws IllegalArgumentException if the id is null or a score is negative
     * @throws java.util.NoSuchElementException if the match is unknown
     * @throws IllegalStateException if the match has already finished
     */
    void updateScore(UUID matchId, int homeScore, int awayScore);

    /**
     * Finishes a match in progress, removing it from the summary.
     *
     * @throws IllegalArgumentException if the id is null
     * @throws java.util.NoSuchElementException if the match is unknown
     * @throws IllegalStateException if the match has already finished
     */
    void finishMatch(UUID matchId);

    /**
     * Returns an unmodifiable snapshot of matches in progress, ordered by total score (descending) and then by
     * start time (most recently started first).
     */
    List<Match> getSummary();
}
