package com.sportradar.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ScoreboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-11T18:00:00Z");

    private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Scoreboard scoreboard = new ScoreboardService(fixedClock);

    @Nested
    class StartMatch {

        @Test
        void shouldStartMatchInProgressWithZeroZeroScore() {
            // Arrange
            // Act
            Match match = scoreboard.startMatch("Mexico", "Canada");

            // Assert
            assertThat(match).extracting(
                            Match::homeTeam, Match::awayTeam, Match::homeScore, Match::awayScore, Match::status)
                    .containsExactly("Mexico", "Canada", 0, 0, MatchStatus.IN_PROGRESS);
        }

        @Test
        void shouldRecordStartInstantFromClock() {
            // Arrange
            // Act
            Match match = scoreboard.startMatch("Mexico", "Canada");

            // Assert
            assertThat(match.startedAt()).isEqualTo(NOW);
        }

        @Test
        void shouldTrimTeamNames() {
            // Arrange
            // Act
            Match match = scoreboard.startMatch("  Mexico ", "\tCanada\n");

            // Assert
            assertThat(match).extracting(Match::homeTeam, Match::awayTeam)
                    .containsExactly("Mexico", "Canada");
        }

        @Test
        void shouldAssignDistinctIdsWhenSameTeamsPlayTwice() {
            // Arrange
            Match first = scoreboard.startMatch("Mexico", "Canada");

            // Act
            Match second = scoreboard.startMatch("Mexico", "Canada");

            // Assert
            assertThat(second.id()).isNotEqualTo(first.id());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldRejectNullOrBlankHomeTeam(String homeTeam) {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.startMatch(homeTeam, "Canada"));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldRejectNullOrBlankAwayTeam(String awayTeam) {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.startMatch("Mexico", awayTeam));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"Mexico, Mexico", "Mexico, mexico", "Mexico,  MEXICO "})
        void shouldRejectTeamPlayingItselfIgnoringCase(String homeTeam, String awayTeam) {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.startMatch(homeTeam, awayTeam));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldNotListMatchWhenStartIsRejected() {
            // Arrange
            catchThrowable(() -> scoreboard.startMatch("Mexico", "Mexico"));

            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary).isEmpty();
        }
    }

    @Nested
    class UpdateScore {

        @Test
        void shouldReplaceScoreOfMatchInProgress() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");

            // Act
            scoreboard.updateScore(match.id(), 0, 5);

            // Assert
            assertThat(scoreboard.getSummary()).singleElement()
                    .extracting(Match::homeScore, Match::awayScore)
                    .containsExactly(0, 5);
        }

        @Test
        void shouldAllowScoreCorrectionDownwards() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.updateScore(match.id(), 3, 2);

            // Act
            scoreboard.updateScore(match.id(), 2, 2);

            // Assert
            assertThat(scoreboard.getSummary()).singleElement()
                    .extracting(Match::homeScore, Match::awayScore)
                    .containsExactly(2, 2);
        }

        @ParameterizedTest
        @CsvSource({"-1, 0", "0, -1", "-1, -1", "-2147483648, 0"})
        void shouldRejectNegativeScores(int homeScore, int awayScore) {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");

            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.updateScore(match.id(), homeScore, awayScore));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldKeepPreviousScoreWhenUpdateIsRejected() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.updateScore(match.id(), 1, 1);
            catchThrowable(() -> scoreboard.updateScore(match.id(), -1, 4));

            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary).singleElement()
                    .extracting(Match::homeScore, Match::awayScore)
                    .containsExactly(1, 1);
        }

        @Test
        void shouldRejectNullMatchId() {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.updateScore(null, 1, 0));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailForUnknownMatch() {
            // Arrange
            UUID unknownId = UUID.randomUUID();

            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.updateScore(unknownId, 1, 0));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        void shouldFailForFinishedMatch() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.finishMatch(match.id());

            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.updateScore(match.id(), 1, 0));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class FinishMatch {

        @Test
        void shouldRemoveFinishedMatchFromSummary() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");

            // Act
            scoreboard.finishMatch(match.id());

            // Assert
            assertThat(scoreboard.getSummary()).isEmpty();
        }

        @Test
        void shouldKeepOtherMatchesInSummary() {
            // Arrange
            Match finished = scoreboard.startMatch("Mexico", "Canada");
            Match inProgress = scoreboard.startMatch("Spain", "Brazil");

            // Act
            scoreboard.finishMatch(finished.id());

            // Assert
            assertThat(scoreboard.getSummary()).extracting(Match::id).containsExactly(inProgress.id());
        }

        @Test
        void shouldRejectNullMatchId() {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.finishMatch(null));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailForUnknownMatch() {
            // Arrange
            UUID unknownId = UUID.randomUUID();

            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.finishMatch(unknownId));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        void shouldFailWhenFinishedTwice() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.finishMatch(match.id());

            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.finishMatch(match.id()));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Summary {

        @Test
        void shouldBeEmptyWhenNoMatchesStarted() {
            // Arrange
            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary).isEmpty();
        }

        @Test
        void shouldOrderAcceptanceScenarioByTotalScoreThenMostRecentlyStarted() {
            // Arrange
            startWithScore("Mexico", "Canada", 0, 5);
            startWithScore("Spain", "Brazil", 10, 2);
            startWithScore("Germany", "France", 2, 2);
            startWithScore("Uruguay", "Italy", 6, 6);
            startWithScore("Argentina", "Australia", 3, 1);

            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary)
                    .extracting(Match::homeTeam, Match::awayTeam, Match::homeScore, Match::awayScore)
                    .containsExactly(
                            tuple("Uruguay", "Italy", 6, 6),
                            tuple("Spain", "Brazil", 10, 2),
                            tuple("Mexico", "Canada", 0, 5),
                            tuple("Argentina", "Australia", 3, 1),
                            tuple("Germany", "France", 2, 2));
        }

        @Test
        void shouldPutMostRecentlyStartedFirstWhenTotalsAreEqualAndInstantsIdentical() {
            // Arrange
            Match first = startWithScore("Mexico", "Canada", 1, 1);
            Match second = startWithScore("Spain", "Brazil", 2, 0);
            Match third = startWithScore("Germany", "France", 0, 2);

            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary).extracting(Match::id)
                    .containsExactly(third.id(), second.id(), first.id());
        }

        @Test
        void shouldPutMostRecentlyStartedFirstWhenTotalsAreEqualAndInstantsDiffer() {
            // Arrange
            MutableClock clock = new MutableClock(NOW);
            Scoreboard tickingScoreboard = new ScoreboardService(clock);
            Match earlier = tickingScoreboard.startMatch("Mexico", "Canada");
            clock.advance(Duration.ofMinutes(5));
            Match later = tickingScoreboard.startMatch("Spain", "Brazil");

            // Act
            List<Match> summary = tickingScoreboard.getSummary();

            // Assert
            assertThat(summary).extracting(Match::id).containsExactly(later.id(), earlier.id());
        }

        @Test
        void shouldRankHigherTotalAheadOfMoreRecentMatch() {
            // Arrange
            Match highScoring = startWithScore("Spain", "Brazil", 4, 4);
            startWithScore("Mexico", "Canada", 0, 1);

            // Act
            List<Match> summary = scoreboard.getSummary();

            // Assert
            assertThat(summary).first().extracting(Match::id).isEqualTo(highScoring.id());
        }

        @Test
        void shouldReturnUnmodifiableList() {
            // Arrange
            startWithScore("Mexico", "Canada", 0, 5);
            List<Match> summary = scoreboard.getSummary();

            // Act
            Throwable thrown = catchThrowable(summary::clear);

            // Assert
            assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldNotChangePreviouslyReturnedSummaryWhenBoardChanges() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            List<Match> summary = scoreboard.getSummary();

            // Act
            scoreboard.updateScore(match.id(), 3, 3);
            scoreboard.finishMatch(match.id());

            // Assert
            assertThat(summary).singleElement()
                    .extracting(Match::homeScore, Match::awayScore)
                    .containsExactly(0, 0);
        }
    }

    @Nested
    class GetMatchById {

        @Test
        void shouldReturnMatchInProgressWithLatestScore() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.updateScore(match.id(), 0, 5);

            // Act
            Optional<Match> found = scoreboard.getMatchById(match.id());

            // Assert
            assertThat(found).get()
                    .extracting(Match::homeScore, Match::awayScore, Match::status)
                    .containsExactly(0, 5, MatchStatus.IN_PROGRESS);
        }

        @Test
        void shouldReturnFinishedMatchWithFinalScore() {
            // Arrange
            Match match = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.updateScore(match.id(), 2, 1);
            scoreboard.finishMatch(match.id());

            // Act
            Optional<Match> found = scoreboard.getMatchById(match.id());

            // Assert
            assertThat(found).get()
                    .extracting(Match::homeScore, Match::awayScore, Match::status)
                    .containsExactly(2, 1, MatchStatus.FINISHED);
        }

        @Test
        void shouldReturnEmptyForUnknownMatch() {
            // Arrange
            scoreboard.startMatch("Mexico", "Canada");

            // Act
            Optional<Match> found = scoreboard.getMatchById(UUID.randomUUID());

            // Assert
            assertThat(found).isEmpty();
        }

        @Test
        void shouldRejectNullMatchId() {
            // Arrange
            // Act
            Throwable thrown = catchThrowable(() -> scoreboard.getMatchById(null));

            // Assert
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private Match startWithScore(String homeTeam, String awayTeam, int homeScore, int awayScore) {
        Match match = scoreboard.startMatch(homeTeam, awayTeam);
        scoreboard.updateScore(match.id(), homeScore, awayScore);
        return match;
    }
}
