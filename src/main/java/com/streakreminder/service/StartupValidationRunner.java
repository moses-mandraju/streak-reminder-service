package com.streakreminder.service;

import com.streakreminder.repository.FirestoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupValidationRunner implements CommandLineRunner {

    private final FirestoreRepository firestoreRepository;

    public StartupValidationRunner(FirestoreRepository firestoreRepository) {
        this.firestoreRepository = firestoreRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Firestore validation...");

        var userIds = firestoreRepository.findAllUserIds();
        log.info("Firebase connection verified. Found {} users.", userIds.size());

        for (String userId : userIds) {
            var habits = firestoreRepository.findHabitsByUserId(userId);
            var tokens = firestoreRepository.findDeviceTokensByUserId(userId);

            log.info("User UID: {}", userId);
            log.info(" - Habit count: {}", habits.size());
            log.info(" - Device token count: {}", tokens.size());
        }

        log.info("Firestore validation completed.");
    }
}
