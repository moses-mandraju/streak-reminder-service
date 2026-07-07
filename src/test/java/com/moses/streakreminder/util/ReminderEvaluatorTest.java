package com.moses.streakreminder.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReminderEvaluatorTest {

    @Test
    void shouldBeDueWhenCurrentMinuteMatchesConfiguredReminderMinuteEvenWithZeroTolerance() {
        var now = ZonedDateTime.of(2026, 7, 7, 17, 25, 59, 0, ZoneId.of("Asia/Calcutta"));

        var due = ReminderEvaluator.isDue(
                "17:25",
                "Asia/Calcutta",
                "Daily",
                List.of(),
                now,
                0
        );

        assertTrue(due);
    }

    @Test
    void shouldNotBeDueWhenDifferentMinuteMatchesConfiguredReminderMinute() {
        var now = ZonedDateTime.of(2026, 7, 7, 17, 26, 0, 0, ZoneId.of("Asia/Calcutta"));

        var due = ReminderEvaluator.isDue(
                "17:25",
                "Asia/Calcutta",
                "Daily",
                List.of(),
                now,
                0
        );

        assertFalse(due);
    }
}
