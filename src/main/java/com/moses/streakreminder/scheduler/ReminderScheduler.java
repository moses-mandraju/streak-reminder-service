package com.moses.streakreminder.scheduler;

import com.moses.streakreminder.model.DueReminder;
import com.moses.streakreminder.model.TaskReminder;
import com.moses.streakreminder.repository.FirestoreRepository;
import com.moses.streakreminder.service.NotificationService;
import com.moses.streakreminder.service.ReminderService;
import com.moses.streakreminder.config.SchedulerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final NotificationService notificationService;
    private final FirestoreRepository firestoreRepository;
    private final SchedulerProperties schedulerProperties;

    public ReminderScheduler(ReminderService reminderService,
                             NotificationService notificationService,
                             FirestoreRepository firestoreRepository,
                             SchedulerProperties schedulerProperties) {
        this.reminderService = reminderService;
        this.notificationService = notificationService;
        this.firestoreRepository = firestoreRepository;
        this.schedulerProperties = schedulerProperties;
    }

    @Scheduled(cron = "${scheduler.cron-expression:0 * * * * *}")
    public void processReminders() {

        log.info("========================================");
        log.info("Reminder Scheduler Started");
        log.info("========================================");

        ZonedDateTime now = ZonedDateTime.now();

        List<DueReminder> dueReminders = reminderService.findDueReminders(now);

        log.info("Due reminders found: {}", dueReminders.size());

        for (DueReminder reminder : dueReminders) {

            var deviceTokens =
                    firestoreRepository.findDeviceTokensByUserId(reminder.getUserId());

            if (deviceTokens.isEmpty()) {
                log.warn("No device tokens found for user {}", reminder.getUserId());
                continue;
            }

            notificationService.sendNotification(
                    deviceTokens,
                    reminder.getNotificationTitle(),
                    reminder.getNotificationMessage()
            );

            log.info(
                    "Reminder sent | User={} | Habit={} | Devices={}",
                    reminder.getUserId(),
                    reminder.getHabitTitle(),
                    deviceTokens.size()
            );
        }

        int carriedForward = reminderService.moveIncompleteTasksForward(now.toInstant());
        log.info("Incomplete tasks moved forward: {}", carriedForward);
        processTaskReminders(now);

        log.info("Reminder Scheduler Completed");
        log.info("========================================");
    }

    private void processTaskReminders(ZonedDateTime now) {
        List<TaskReminder> dueTasks = reminderService.claimDueTaskReminders(
                now,
                schedulerProperties.getTaskReminderGraceSeconds());
        log.info("Due task reminders claimed: {}", dueTasks.size());

        for (TaskReminder task : dueTasks) {
            if (!reminderService.isTaskReminderClaimCurrent(task)) {
                continue;
            }

            var deviceTokens = firestoreRepository.findDeviceTokensByUserId(task.getUserId());
            if (deviceTokens.isEmpty()) {
                reminderService.finishTaskReminder(task, "pending", Instant.now());
                log.warn("No device tokens available for task reminder user={}, task={}",
                        task.getUserId(), task.getId());
                continue;
            }

            boolean sent = notificationService.sendTaskNotification(task, deviceTokens);
            reminderService.finishTaskReminder(
                    task,
                    sent ? "sent" : "pending",
                    Instant.now());
        }
    }
}