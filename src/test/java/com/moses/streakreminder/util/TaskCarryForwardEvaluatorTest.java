package com.moses.streakreminder.util;

import com.moses.streakreminder.model.TaskReminder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCarryForwardEvaluatorTest {

    @Test
    void movesOptedInIncompleteTasksToTheCurrentLocalDate() {
        var task = task("2026-11-20", true, false, "Asia/Kolkata");

        var evaluation = TaskCarryForwardEvaluator.evaluate(
                task,
                Instant.parse("2026-11-20T18:31:00Z"));

        assertEquals(TaskCarryForwardEvaluator.State.DUE, evaluation.state());
        assertEquals(LocalDate.parse("2026-11-21"), evaluation.targetDate());
    }

    @Test
    void waitsForTheTaskDateAndIgnoresTasksWithoutExplicitOptIn() {
        var now = Instant.parse("2026-11-21T06:30:00Z");

        assertEquals(
                TaskCarryForwardEvaluator.State.NOT_DUE,
                TaskCarryForwardEvaluator.evaluate(
                        task("2026-11-21", true, false, "Asia/Kolkata"), now).state());
        assertEquals(
                TaskCarryForwardEvaluator.State.INACTIVE,
                TaskCarryForwardEvaluator.evaluate(
                        task("2026-11-20", false, false, "Asia/Kolkata"), now).state());
        assertEquals(
                TaskCarryForwardEvaluator.State.INACTIVE,
                TaskCarryForwardEvaluator.evaluate(
                        task("2026-11-20", true, true, "Asia/Kolkata"), now).state());
    }

    @Test
    void evaluatesRolloverUsingEachTaskTimezoneAndRejectsInvalidDates() {
        var utcNow = Instant.parse("2026-11-20T23:30:00Z");

        assertEquals(
                TaskCarryForwardEvaluator.State.DUE,
                TaskCarryForwardEvaluator.evaluate(
                        task("2026-11-20", true, false, "Asia/Tokyo"), utcNow).state());
        assertEquals(
                TaskCarryForwardEvaluator.State.NOT_DUE,
                TaskCarryForwardEvaluator.evaluate(
                        task("2026-11-20", true, false, "America/Los_Angeles"), utcNow).state());
        assertEquals(
                TaskCarryForwardEvaluator.State.INVALID,
                TaskCarryForwardEvaluator.evaluate(
                        task("not-a-date", true, false, "UTC"), utcNow).state());
    }

    private static TaskReminder task(
            String dueDate,
            boolean carryForward,
            boolean completed,
            String timezone) {
        return TaskReminder.builder()
                .id("task-1")
                .dueDate(dueDate)
                .carryForward(carryForward)
                .completed(completed)
                .timezone(timezone)
                .build();
    }
}
