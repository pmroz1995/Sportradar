package com.sportradar.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchTest {

    private static final Instant STARTED_AT = Instant.parse("2026-06-11T18:00:00Z");

    @Test
    void shouldStartInProgressWithZeroScore() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        Match match = Match.started(id, "Mexico", "Canada", STARTED_AT);

        // Assert
        assertThat(match).isEqualTo(
                new Match(id, "Mexico", "Canada", 0, 0, MatchStatus.IN_PROGRESS, STARTED_AT));
    }

    @Test
    void shouldSumScoresWithoutOverflow() {
        // Arrange
        Match match = Match.started(UUID.randomUUID(), "Mexico", "Canada", STARTED_AT)
                .withScore(Integer.MAX_VALUE, Integer.MAX_VALUE);

        // Act
        long totalScore = match.totalScore();

        // Assert
        assertThat(totalScore).isEqualTo(2L * Integer.MAX_VALUE);
    }

    @Test
    void shouldRejectScoreUpdateWhenFinished() {
        // Arrange
        Match finished = Match.started(UUID.randomUUID(), "Mexico", "Canada", STARTED_AT).finished();

        // Act
        Throwable thrown = catchThrowable(() -> finished.withScore(1, 0));

        // Assert
        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(finished.id().toString());
    }

    @Test
    void shouldRejectFinishingTwice() {
        // Arrange
        Match finished = Match.started(UUID.randomUUID(), "Mexico", "Canada", STARTED_AT).finished();

        // Act
        Throwable thrown = catchThrowable(finished::finished);

        // Assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }
}
