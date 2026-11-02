package com.sportradar.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ScoreboardServiceConcurrencyTest {

    private static final int THREADS = 32;

    private final Scoreboard scoreboard = new ScoreboardService();

    @Test
    void shouldListEveryMatchStartedConcurrently() throws Exception {
        // Arrange
        AtomicInteger counter = new AtomicInteger();

        // Act
        List<Match> started = runConcurrently(
                () -> scoreboard.startMatch("Home " + counter.incrementAndGet(), "Away"));

        // Assert
        assertThat(scoreboard.getSummary()).containsExactlyInAnyOrderElementsOf(started);
    }

    @Test
    void shouldFinishMatchExactlyOnceWhenFinishedConcurrently() throws Exception {
        // Arrange
        UUID matchId = scoreboard.startMatch("Mexico", "Canada").id();

        // Act
        List<Throwable> outcomes = runConcurrently(
                () -> catchThrowable(() -> scoreboard.finishMatch(matchId)));

        // Assert
        assertThat(outcomes).filteredOn(Objects::isNull).hasSize(1);
        assertThat(outcomes).filteredOn(Objects::nonNull)
                .hasSize(THREADS - 1)
                .allMatch(IllegalStateException.class::isInstance);
    }

    @Test
    void shouldNeverExposeTornScoreWhenUpdatedConcurrently() throws Exception {
        // Arrange
        UUID matchId = scoreboard.startMatch("Mexico", "Canada").id();
        AtomicInteger counter = new AtomicInteger();

        // Act
        runConcurrently(() -> {
            int score = counter.incrementAndGet();
            scoreboard.updateScore(matchId, score, score);
            return score;
        });

        // Assert
        assertThat(scoreboard.getSummary()).singleElement()
                .satisfies(match -> assertThat(match.homeScore()).isEqualTo(match.awayScore()));
    }

    private static <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();
            return collect(futures);
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<T> collect(List<Future<T>> futures) throws Exception {
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }
}
