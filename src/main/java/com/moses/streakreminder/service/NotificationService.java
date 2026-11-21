package com.moses.streakreminder.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.moses.streakreminder.model.DeviceToken;
import com.moses.streakreminder.model.TaskReminder;
import com.moses.streakreminder.repository.FirestoreRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    // Change signature to accept uid
    public String sendNotification(String uid, String token, String title, String body) {
        return sendNotification(
                uid,
                token,
                title,
                body,
                "reminder-" + System.currentTimeMillis(),
                null);
    }

    private String sendNotification(
            String uid,
            String token,
            String title,
            String body,
            String uniqueTag,
            String taskId) {
        try {
            var messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("tag", uniqueTag)
                    .setWebpushConfig(WebpushConfig.builder()
                            .putHeader("Urgency", "high")
                            .putHeader("TTL", "60")
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setTag(uniqueTag)
                                    .build())
                            .build());
            if (taskId != null) {
                messageBuilder.putData("taskId", taskId);
            }

            Message message = messageBuilder.build();
            String messageId = firebaseMessaging.send(message);
            log.info("Notification sent successfully. MessageId: {}", messageId);
            return messageId;

        } catch (FirebaseMessagingException ex) {
            if (MessagingErrorCode.UNREGISTERED.equals(ex.getMessagingErrorCode())) {
                log.warn("Stale device token detected for user {}; removing it.", uid);
                firestoreRepository.deleteDeviceToken(uid, token);
            } else {
                log.error("Failed to send notification for user={}", uid, ex);
            }
            throw new RuntimeException("Unable to send notification", ex);
        }
    }

    public boolean sendTaskNotification(
            TaskReminder task,
            Iterable<DeviceToken> deviceTokens) {
        String title = "Task reminder";
        String body = Objects.requireNonNullElse(task.getTitle(), "Your scheduled task");
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            body += " — " + task.getDescription();
        }
        String tag = String.format(
                "task-%s-%s-%s-%d",
                task.getId(),
                task.getDueDate(),
                task.isAllDay() ? "all-day" : task.getStartTime(),
                task.getReminderMinutesBefore());
        int successfulSends = 0;

        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken == null || deviceToken.getToken() == null || deviceToken.getToken().isBlank()) {
                continue;
            }
            try {
                sendNotification(
                        task.getUserId(),
                        deviceToken.getToken(),
                        title,
                        body,
                        tag,
                        task.getId());
                successfulSends++;
            } catch (RuntimeException exception) {
                log.warn("Skipping task reminder for user={} after device send failed.", task.getUserId());
            }
        }

        return successfulSends > 0;
    }

    public void sendNotification(Iterable<DeviceToken> deviceTokens, String title, String body) {
        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken == null || deviceToken.getToken() == null || deviceToken.getToken().isBlank()) {
                continue;
            }
            try {
                sendNotification(deviceToken.getUid(), deviceToken.getToken(), title, body);
            } catch (RuntimeException ex) {
                log.warn("Skipping device token after notification send failed.");
            }
        }
    }
}