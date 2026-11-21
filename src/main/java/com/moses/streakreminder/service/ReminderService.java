package com.moses.streakreminder.service;

import com.moses.streakreminder.model.DueReminder;
import com.moses.streakreminder.model.Habit;
import com.moses.streakreminder.model.TaskReminder;
import com.moses.streakreminder.repository.FirestoreRepository;
import com.moses.streakreminder.util.ReminderEvaluator;
import com.moses.streakreminder.util.TaskReminderEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Slf4j
@Service
public class ReminderService {

    private final FirestoreRepository firestoreRepository;

    public ReminderService(FirestoreRepository firestoreRepository) {
        this.firestoreRepository = firestoreRepository;
    }

    public List<DueReminder> findDueReminders(ZonedDateTime now) {
        var dueReminders = new ArrayList<DueReminder>();
        var userIds = firestoreRepository.findAllUserIds();

        for (var userId : userIds) {
            var habits = firestoreRepository.findHabitsByUserId(userId);
            for (var habit : habits) {
                var due = shouldEvaluate(habit) && ReminderEvaluator.isDue(
                        habit.getReminderTime(),
                        habit.getReminderTimezone(),
                        habit.getReminderFrequency(),
                        habit.getSelectedDays(),
                        now,
                        ReminderEvaluator.DEFAULT_TOLERANCE_SECONDS);
                logEvaluation(userId, habit, now, due);
                if (due) {
                    dueReminders.add(buildDueReminder(userId, habit));
                }
            }
        }

        return dueReminders;
    }

    public List<TaskReminder> claimDueTaskReminders(ZonedDateTime now, long graceSeconds) {
        var dueTasks = new ArrayList<TaskReminder>();
        Instant nowInstant = now.toInstant();

        for (var userId : firestoreRepository.findAllUserIds()) {
            for (var task : firestoreRepository.findTasksByUserId(userId)) {
                var evaluation = TaskReminderEvaluator.evaluate(task, nowInstant, graceSeconds);
                if (evaluation.state() != TaskReminderEvaluator.State.DUE
                        && evaluation.state() != TaskReminderEvaluator.State.EXPIRED
                        && evaluation.state() != TaskReminderEvaluator.State.INVALID
                        && !"sending".equals(task.getReminderStatus())) {
                    continue;
                }

                var claimedTask = firestoreRepository.claimTaskReminder(
                        userId,
                        task.getId(),
                        nowInstant,
                        graceSeconds);
                if (claimedTask != null) {
                    dueTasks.add(claimedTask.toBuilder().userId(userId).build());
                }
            }
        }

        return dueTasks;
    }

    public boolean isTaskReminderClaimCurrent(TaskReminder task) {
        return firestoreRepository.isTaskReminderClaimCurrent(task.getUserId(), task);
    }

    public boolean finishTaskReminder(TaskReminder task, String status, Instant now) {
        return firestoreRepository.finishTaskReminder(
                task.getUserId(),
                task.getId(),
                status,
                now);
    }

    private static boolean shouldEvaluate(Habit habit) {
        return habit.isReminderEnabled();
    }

    private static DueReminder buildDueReminder(String userId, Habit habit) {
        var title = blankToDefault(habit.getNotificationTitle(), "Time for %s", habit.getTitle());
        var message = blankToDefault(habit.getNotificationMessage(), "Don't break your streak today!");

        return DueReminder.builder()
                .userId(userId)
                .habitId(habit.getId())
                .habitTitle(habit.getTitle())
                .emoji(habit.getEmoji())
                .notificationTitle(title)
                .notificationMessage(message)
                .reminderTime(habit.getReminderTime())
                .reminderTimezone(habit.getReminderTimezone())
                .build();
    }

    private static String blankToDefault(String value, String defaultTemplate, Object... args) {
        if (value == null || value.isBlank()) {
            return String.format(defaultTemplate, args);
        }
        return value;
    }

    private void logEvaluation(String userId, Habit habit, ZonedDateTime now, boolean due) {
        log.info("User ID={}, Habit='{}', ReminderEnabled={}, ReminderTime={}, ReminderFrequency={}, CurrentLocalTime={}, Timezone={}, Due={}",
                userId,
                habit.getTitle(),
                habit.isReminderEnabled(),
                habit.getReminderTime(),
                habit.getReminderFrequency(),
                now.withZoneSameInstant(getZoneIdOrDefault(habit.getReminderTimezone())),
                habit.getReminderTimezone(),
                due);
    }

    private static java.time.ZoneId getZoneIdOrDefault(String timezone) {
        try {
            return java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            return java.time.ZoneId.systemDefault();
        }
    }
}
