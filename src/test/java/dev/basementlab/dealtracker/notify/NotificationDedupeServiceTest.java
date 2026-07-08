package dev.basementlab.dealtracker.notify;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDedupeServiceTest {

    private final Instant now = Instant.parse("2026-07-08T12:00:00Z");

    @Test
    void sendsWhenNeverSentBefore() {
        boolean result = NotificationDedupeService.shouldSend(Optional.empty(), 24, now);

        assertThat(result).isTrue();
    }

    @Test
    void withholdsWhenInsideCooldownWindow() {
        Instant sentTwoHoursAgo = now.minus(2, ChronoUnit.HOURS);

        boolean result = NotificationDedupeService.shouldSend(Optional.of(sentTwoHoursAgo), 24, now);

        assertThat(result).isFalse();
    }

    @Test
    void sendsOnceCooldownWindowHasElapsed() {
        Instant sentTwentyFiveHoursAgo = now.minus(25, ChronoUnit.HOURS);

        boolean result = NotificationDedupeService.shouldSend(Optional.of(sentTwentyFiveHoursAgo), 24, now);

        assertThat(result).isTrue();
    }

    @Test
    void sendsExactlyAtCooldownBoundary() {
        Instant sentExactlyTwentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);

        boolean result = NotificationDedupeService.shouldSend(Optional.of(sentExactlyTwentyFourHoursAgo), 24, now);

        assertThat(result).isTrue();
    }
}
