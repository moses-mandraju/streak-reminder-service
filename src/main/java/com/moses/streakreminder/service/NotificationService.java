package com.moses.streakreminder.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.moses.streakreminder.model.DeviceToken;
import com.moses.streakreminder.repository.FirestoreRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final FirestoreRepository firestoreRepository; // inject your existing repo

    public NotificationService(FirebaseMessaging firebaseMessaging,
                                FirestoreRepository firestoreRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.firestoreRepository = firestoreRepository;
    }

    public String sendNotification(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("Notification sent successfully. MessageId: {}, Token: {}", messageId, token);
            return messageId;

        } catch (FirebaseMessagingException ex) {

            if (MessagingErrorCode.UNREGISTERED.equals(ex.getMessagingErrorCode())) {
                log.warn("Stale token detected, removing from Firestore: {}", token);
                firestoreRepository.deleteDeviceToken(token); // 👈 delete it
            } else {
                log.error("Failed to send notification to token={}", token, ex);
            }

            throw new RuntimeException("Unable to send notification", ex);
        }
    }

    public void sendNotification(Iterable<DeviceToken> deviceTokens, String title, String body) {
        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken.getToken() == null || deviceToken.getToken().isBlank()) {
                continue;
            }
            try {
                sendNotification(deviceToken.getToken(), title, body);
            } catch (RuntimeException ex) {
                // Log and continue — don't let one bad token stop others
                log.warn("Skipping token due to error: {}", deviceToken.getToken());
            }
        }
    }
}