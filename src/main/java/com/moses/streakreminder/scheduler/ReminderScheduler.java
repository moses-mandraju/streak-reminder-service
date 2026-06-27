package com.moses.streakreminder.scheduler;

import com.moses.streakreminder.model.DueReminder;
import com.moses.streakreminder.repository.FirestoreRepository;
import com.moses.streakreminder.service.NotificationService;
import com.moses.streakreminder.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final NotificationService notificationService;
    private final FirestoreRepository firestoreRepository;

    public ReminderScheduler(ReminderService reminderService,
                             NotificationService notificationService,
                             FirestoreRepository firestoreRepository) {
        this.reminderService = reminderService;
        this.notificationService = notificationService;
        this.firestoreRepository = firestoreRepository;
    }

    @Scheduled(fixedDelayString = "${scheduler.poll-interval-seconds:60}000")
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

        log.info("Reminder Scheduler Completed");
        log.info("========================================");
    }
}