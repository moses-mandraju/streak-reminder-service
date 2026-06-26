package com.moses.streakreminder.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReminderEvaluator {

    public static final long DEFAULT_TOLERANCE_SECONDS = 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static boolean isDue(String reminderTime,
                                String reminderTimezone,
                                String reminderFrequency,
                                List<String> selectedDays,
                                ZonedDateTime now,
                                long toleranceSeconds) {
        var configuredTime = parseTime(reminderTime);
        if (configuredTime == null) {
            log.warn("Skipping invalid reminderTime='{}'", reminderTime);
            return false;
        }

        var zoneId = parseZoneId(reminderTimezone);
        if (zoneId == null) {
            log.warn("Skipping invalid reminderTimezone='{}'", reminderTimezone);
            return false;
        }

        var localNow = now.withZoneSameInstant(zoneId);
        log.debug("Evaluating reminder in timezone {} local time {}", reminderTimezone, localNow);

        if (isInToleranceWindow(configuredTime, localNow.toLocalTime(), toleranceSeconds)) {
            return evaluateFrequency(reminderFrequency, selectedDays, localNow);
        }

        return false;
    }

    private static LocalTime parseTime(String reminderTime) {
        try {
            return LocalTime.parse(reminderTime, TIME_FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private static ZoneId parseZoneId(String reminderTimezone) {
        try {
            return ZoneId.of(reminderTimezone);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isInToleranceWindow(LocalTime configuredTime, LocalTime actualTime, long toleranceSeconds) {
        var secondsDifference = Math.abs(configuredTime.toSecondOfDay() - actualTime.toSecondOfDay());
        return secondsDifference <= toleranceSeconds;
    }

    private static boolean evaluateFrequency(String reminderFrequency,
                                             List<String> selectedDays,
                                             ZonedDateTime localNow) {
        if (reminderFrequency == null || reminderFrequency.isBlank() || reminderFrequency.equalsIgnoreCase("Daily")) {
            return true;
        }

        if (reminderFrequency.equalsIgnoreCase("Weekly")) {
            return isWeeklyDue(selectedDays, localNow);
        }

        log.warn("Unsupported reminderFrequency='{}', defaulting to false", reminderFrequency);
        return false;
    }

    private static boolean isWeeklyDue(List<String> selectedDays, ZonedDateTime localNow) {
        if (selectedDays == null || selectedDays.isEmpty()) {
            log.warn("Skipping weekly reminder with empty selectedDays");
            return false;
        }

        var currentDay = localNow.getDayOfWeek();
        return selectedDays.stream()
                .map(ReminderEvaluator::normalizeDay)
                .filter(java.util.Objects::nonNull)
                .anyMatch(day -> day == currentDay);
    }

    private static DayOfWeek normalizeDay(String rawDay) {
        if (rawDay == null || rawDay.isBlank()) {
            return null;
        }

        try {
            return DayOfWeek.valueOf(rawDay.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Skipping unsupported selectedDay='{}'", rawDay);
            return null;
        }
    }
}
