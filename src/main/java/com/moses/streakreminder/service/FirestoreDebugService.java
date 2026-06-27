package com.moses.streakreminder.service;

import com.moses.streakreminder.model.*;
import com.moses.streakreminder.repository.FirestoreRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FirestoreDebugService {

    private final FirestoreRepository firestoreRepository;

    private final NotificationService notificationService;

    public FirestoreDebugService(FirestoreRepository firestoreRepository, NotificationService notificationService) {
        this.firestoreRepository = firestoreRepository;
        this.notificationService = notificationService;
    }

    public FirestoreSnapshotResponse getFirestoreSnapshot() {

        List<String> userIds = firestoreRepository.findAllUserIds();

        List<UserSnapshot> users = new ArrayList<>();

        for (String userId : userIds) {

            UserSnapshot snapshot = firestoreRepository.findUserSnapshot(userId);

            if (snapshot != null) {
                users.add(snapshot);
            }
        }

        return FirestoreSnapshotResponse.builder()
                .totalUsers(users.size())
                .users(users)
                .build();
    }

    public String sendTestNotification(String userId) {

    var deviceTokens = firestoreRepository.findDeviceTokensByUserId(userId);

    if (deviceTokens.isEmpty()) {
        return "No device tokens found.";
    }

    notificationService.sendNotification(
            deviceTokens,
            "Spring Boot Test 🚀",
            "Congratulations! Your first notification from Spring Boot is working."
    );

    return "Notification sent to " + deviceTokens.size() + " device(s).";
}
}