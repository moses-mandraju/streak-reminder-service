package com.moses.streakreminder.util;

import com.moses.streakreminder.model.TaskReminder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskReminderEvaluatorTest {

    @Test
    void evaluatesTaskTimeInItsTimezoneAcrossDaylightSavingTime() {
        var task = task("2026-03-08", "09:00", false, 10, "America/Los_Angeles");
        var evaluation = TaskReminderEvaluator.evaluate(
                task,
                Instant.parse("2026-03-08T15:51:00Z"),
                120);

        assertEquals(TaskReminderEvaluator.State.DUE, evaluation.state());
        assertEquals(Instant.parse("2026-03-08T15:50:00Z"), evaluation.scheduledFor());
    }

    @Test
    void usesNineAmLocalTimeForAnAllDayReminder() {
        var task = task("2026-11-21", "", true, 0, "Asia/Kolkata");
        var evaluation = TaskReminderEvaluator.evaluate(
                task,
                Instant.parse("2026-11-21T03:30:30Z"),
                120);

        assertEquals(TaskReminderEvaluator.State.DUE, evaluation.state());
        assertEquals(Instant.parse("2026-11-21T03:30:00Z"), evaluation.scheduledFor());
    }

    @Test
    void ignoresFutureTimesAndExpiresStaleReminderTimes() {
        var task = task("2026-11-21", "10:00", false, 0, "Asia/Kolkata");

        assertEquals(
                TaskReminderEvaluator.State.NOT_DUE,
                TaskReminderEvaluator.evaluate(
                        task,
                        Instant.parse("2026-11-21T04:29:00Z"),
                        120).state());
        assertEquals(
                TaskReminderEvaluator.State.EXPIRED,
                TaskReminderEvaluator.evaluate(
                        task,
                        Instant.parse("2026-11-21T05:03:00Z"),
                        120).state());
    }

    @Test
    void neverSendsCompletedDisabledOrMalformedTaskReminders() {
        var now = Instant.parse("2026-11-21T04:30:00Z");
        var task = task("2026-11-21", "10:00", false, 0, "Asia/Kolkata");
        assertEquals(
                TaskReminderEvaluator.State.INACTIVE,
                TaskReminderEvaluator.evaluate(
                        task.toBuilder().completed(true).build(),
                        now,
                        120).state());
        assertEquals(
                TaskReminderEvaluator.State.INACTIVE,
                TaskReminderEvaluator.evaluate(
                        task.toBuilder().reminderStatus("cancelled").build(),
                        now,
                        120).state());
        assertEquals(
                TaskReminderEvaluator.State.INVALID,
                TaskReminderEvaluator.evaluate(
                        task("2026-11-21", "10:00", false, 7, "Asia/Kolkata"),
                        now,
                        120).state());
        assertEquals(
                TaskReminderEvaluator.State.INVALID,
                TaskReminderEvaluator.evaluate(
                        task("2026-11-21", "10:00", false, 0, "Not/AZone"),
                        now,
                        120).state());
    }

    private static TaskReminder task(
            String dueDate,
            String startTime,
            boolean allDay,
            int minutesBefore,
            String timezone) {
        return TaskReminder.builder()
                .id("task-1")
                .title("Example task")
                .dueDate(dueDate)
                .startTime(startTime)
                .allDay(allDay)
                .notificationEnabled(true)
                .reminderMinutesBefore(minutesBefore)
                .reminderStatus("pending")
                .timezone(timezone)
                .build();
    }
}
