package com.moses.streakreminder.util;

import com.moses.streakreminder.model.TaskReminder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskReminderEvaluator {

    private static final Set<Integer> ALLOWED_MINUTES_BEFORE = Set.of(0, 5, 10, 15, 30, 60);
    private static final LocalTime ALL_DAY_REMINDER_TIME = LocalTime.of(9, 0);

    public enum State {
        INACTIVE,
        NOT_DUE,
        DUE,
        EXPIRED,
        INVALID
    }

    public record Evaluation(State state, Instant scheduledFor) {
    }

    public static Evaluation evaluate(TaskReminder task, Instant now, long graceSeconds) {
        if (task == null
                || task.isCompleted()
                || !task.isNotificationEnabled()
                || (task.getReminderStatus() != null
                    && !task.getReminderStatus().isBlank()
                    && !task.getReminderStatus().equals("pending"))) {
            return new Evaluation(State.INACTIVE, null);
        }

        try {
            var zoneId = ZoneId.of(task.getTimezone());
            var date = LocalDate.parse(task.getDueDate());
            var startTime = task.isAllDay()
                    ? ALL_DAY_REMINDER_TIME
                    : LocalTime.parse(task.getStartTime());
            if (!ALLOWED_MINUTES_BEFORE.contains(task.getReminderMinutesBefore())) {
                return new Evaluation(State.INVALID, null);
            }

            var trigger = date.atTime(startTime)
                    .atZone(zoneId)
                    .minus(Duration.ofMinutes(task.getReminderMinutesBefore()))
                    .toInstant();
            var secondsLate = Duration.between(trigger, now).getSeconds();

            if (secondsLate < 0) {
                return new Evaluation(State.NOT_DUE, trigger);
            }
            if (secondsLate > Math.max(graceSeconds, 0)) {
                return new Evaluation(State.EXPIRED, trigger);
            }
            return new Evaluation(State.DUE, trigger);
        } catch (java.time.DateTimeException | NullPointerException exception) {
            return new Evaluation(State.INVALID, null);
        }
    }
}
