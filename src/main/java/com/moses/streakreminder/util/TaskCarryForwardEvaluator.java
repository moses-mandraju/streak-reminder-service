package com.moses.streakreminder.util;

import com.moses.streakreminder.model.TaskReminder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskCarryForwardEvaluator {

    public enum State {
        INACTIVE,
        NOT_DUE,
        DUE,
        INVALID
    }

    public record Evaluation(State state, LocalDate targetDate) {
    }

    public static Evaluation evaluate(TaskReminder task, Instant now) {
        if (task == null || task.isCompleted() || !task.isCarryForward()) {
            return new Evaluation(State.INACTIVE, null);
        }

        try {
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            LocalDate localToday = LocalDate.ofInstant(now, ZoneId.of(task.getTimezone()));
            return dueDate.isBefore(localToday)
                    ? new Evaluation(State.DUE, localToday)
                    : new Evaluation(State.NOT_DUE, null);
        } catch (DateTimeException | NullPointerException exception) {
            return new Evaluation(State.INVALID, null);
        }
    }
}
