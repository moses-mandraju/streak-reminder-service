package com.moses.streakreminder.service;

import com.moses.streakreminder.model.DueReminder;
import com.moses.streakreminder.repository.FirestoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class StartupValidationRunner implements CommandLineRunner {

    private final FirestoreRepository firestoreRepository;
    private final ReminderService reminderService;

    public StartupValidationRunner(FirestoreRepository firestoreRepository, ReminderService reminderService) {
        this.firestoreRepository = firestoreRepository;
        this.reminderService = reminderService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Firestore validation...");

        var userIds = firestoreRepository.findAllUserIds();
        log.info("Firebase connection verified. Found {} users.", userIds.size());

        int totalHabits = userIds.stream()
                .mapToInt(userId -> firestoreRepository.findHabitsByUserId(userId).size())
                .sum();

        var now = ZonedDateTime.now(ZoneId.systemDefault());
        var dueReminders = reminderService.findDueReminders(now);

        log.info("---------------------------------------");
        log.info("Current Time: {}", now.format(DateTimeFormatter.ofPattern("HH:mm")));
        log.info("Total Users: {}", userIds.size());
        log.info("Total Habits: {}", totalHabits);
        log.info("Due Reminders: {}", dueReminders.size());

        for (DueReminder dueReminder : dueReminders) {
            log.info("---------------------------------------");
            log.info("User: {}", dueReminder.getUserId());
            log.info("Habit: {}", dueReminder.getHabitTitle());
            log.info("Reminder: {}", dueReminder.getReminderTime());
            log.info("Timezone: {}", dueReminder.getReminderTimezone());
            log.info("Status: DUE");
        }

        log.info("---------------------------------------");
        log.info("Firestore validation completed.");
    }
}

