package com.moses.streakreminder.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.moses.streakreminder.model.DeviceToken;
import com.moses.streakreminder.model.Habit;
import com.moses.streakreminder.model.TaskReminder;
import com.moses.streakreminder.model.UserSnapshot;
import com.moses.streakreminder.util.FirestoreMapper;
import com.moses.streakreminder.util.TaskReminderEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FirestoreRepository {

    private static final long TASK_REMINDER_CLAIM_TIMEOUT_SECONDS = 30;
    private final Firestore firestore;

    public FirestoreRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<String> findAllUserIds() {
        try {
            QuerySnapshot snapshot = firestore.collection("users").get().get();

            log.info("====================================");
            log.info("Documents returned = {}", snapshot.size());

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                log.info("Document ID = {}", doc.getId());
                log.info("Exists = {}", doc.exists());
                log.info("Data = {}", doc.getData());
            }

            log.info("====================================");

            return snapshot.getDocuments()
                    .stream()
                    .map(DocumentSnapshot::getId)
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to retrieve user ids from Firestore; skipping scheduler cycle", e);
            return List.of();
        }
    }

    public List<Habit> findHabitsByUserId(String userId) {
        try {
            CollectionReference habits = firestore.collection("users").document(userId).collection("habits");
            ApiFuture<QuerySnapshot> habitsFuture = habits.get();
            QuerySnapshot habitsSnapshot = habitsFuture.get();
            return habitsSnapshot.getDocuments().stream()
                    .map(FirestoreMapper::toHabit)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Unable to retrieve habits for user {}; skipping", userId, e);
            return List.of();
        }
    }

    public List<DeviceToken> findDeviceTokensByUserId(String userId) {
        try {
            CollectionReference tokens = firestore.collection("users").document(userId).collection("deviceTokens");
            ApiFuture<QuerySnapshot> tokensFuture = tokens.get();
            QuerySnapshot tokensSnapshot = tokensFuture.get();
            return tokensSnapshot.getDocuments().stream()
                    .map(FirestoreMapper::toDeviceToken)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Unable to retrieve device tokens for user {}; skipping", userId, e);
            return List.of();
        }
    }

    public List<TaskReminder> findTasksByUserId(String userId) {
        try {
            CollectionReference tasks = firestore.collection("users")
                    .document(userId)
                    .collection("tasks");
            ApiFuture<QuerySnapshot> tasksFuture = tasks.get();
            QuerySnapshot tasksSnapshot = tasksFuture.get();
            return tasksSnapshot.getDocuments().stream()
                    .map(FirestoreMapper::toTaskReminder)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Unable to retrieve tasks for user {}; skipping", userId, exception);
            return List.of();
        } catch (ExecutionException exception) {
            log.warn("Unable to retrieve tasks for user {}; skipping", userId, exception);
            return List.of();
        }
    }

    public boolean moveTaskForwardIfStillDue(
            String userId,
            TaskReminder task,
            LocalDate targetDate,
            Instant now) {
        DocumentReference taskReference = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(task.getId());

        try {
            return firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(taskReference).get();
                TaskReminder currentTask = FirestoreMapper.toTaskReminder(snapshot);
                if (currentTask == null
                        || currentTask.isCompleted()
                        || !currentTask.isCarryForward()
                        || !Objects.equals(currentTask.getDueDate(), task.getDueDate())
                        || currentTask.getDueDate().compareTo(targetDate.toString()) >= 0) {
                    return false;
                }

                transaction.update(taskReference, Map.of(
                        "dueDate", targetDate.toString(),
                        "reminderStatus", currentTask.isNotificationEnabled() ? "pending" : "disabled",
                        "reminderClaimedAt", "",
                        "reminderSentAt", "",
                        "updatedAt", now.toString()));
                return true;
            }).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while moving task forward for user={}, task={}",
                    userId, task.getId(), exception);
            return false;
        } catch (ExecutionException exception) {
            log.error("Unable to move task forward for user={}, task={}",
                    userId, task.getId(), exception);
            return false;
        }
    }

    public TaskReminder claimTaskReminder(
            String userId,
            String taskId,
            Instant now,
            long graceSeconds) {
        DocumentReference taskReference = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId);

        try {
            return firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(taskReference).get();
                TaskReminder task = FirestoreMapper.toTaskReminder(snapshot);
                if (task == null) {
                    return null;
                }

                var taskToEvaluate = task;
                if ("sending".equals(task.getReminderStatus())) {
                    if (!isTaskReminderClaimStale(snapshot.getString("reminderClaimedAt"), now)) {
                        return null;
                    }
                    taskToEvaluate = task.toBuilder().reminderStatus("pending").build();
                }

                var evaluation = TaskReminderEvaluator.evaluate(taskToEvaluate, now, graceSeconds);
                if (evaluation.state() == TaskReminderEvaluator.State.DUE) {
                    transaction.update(taskReference, Map.of(
                            "reminderStatus", "sending",
                            "reminderClaimedAt", now.toString(),
                            "updatedAt", now.toString()));
                    return task;
                }

                if (evaluation.state() == TaskReminderEvaluator.State.EXPIRED
                        || evaluation.state() == TaskReminderEvaluator.State.INVALID) {
                    transaction.update(taskReference, Map.of(
                            "reminderStatus",
                            evaluation.state() == TaskReminderEvaluator.State.EXPIRED
                                    ? "missed"
                                    : "invalid",
                            "updatedAt",
                            now.toString()));
                }
                return null;
            }).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while claiming task reminder for user={}, task={}", userId, taskId, exception);
            return null;
        } catch (ExecutionException exception) {
            log.error("Unable to claim task reminder for user={}, task={}", userId, taskId, exception);
            return null;
        }
    }

    private static boolean isTaskReminderClaimStale(String claimedAt, Instant now) {
        try {
            return Instant.parse(claimedAt)
                    .isBefore(now.minusSeconds(TASK_REMINDER_CLAIM_TIMEOUT_SECONDS));
        } catch (DateTimeParseException | NullPointerException exception) {
            return true;
        }
    }

    public boolean isTaskReminderClaimCurrent(String userId, TaskReminder claimedTask) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(claimedTask.getId())
                    .get()
                    .get();
            TaskReminder currentTask = FirestoreMapper.toTaskReminder(snapshot);
            return currentTask != null
                    && currentTask.isNotificationEnabled()
                    && !currentTask.isCompleted()
                    && "sending".equals(currentTask.getReminderStatus())
                    && Objects.equals(currentTask.getTitle(), claimedTask.getTitle())
                    && Objects.equals(currentTask.getDescription(), claimedTask.getDescription())
                    && Objects.equals(currentTask.getDueDate(), claimedTask.getDueDate())
                    && Objects.equals(currentTask.getStartTime(), claimedTask.getStartTime())
                    && currentTask.isAllDay() == claimedTask.isAllDay()
                    && currentTask.getReminderMinutesBefore() == claimedTask.getReminderMinutesBefore()
                    && Objects.equals(currentTask.getTimezone(), claimedTask.getTimezone());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while checking task reminder claim for user={}, task={}",
                    userId, claimedTask.getId(), exception);
            return false;
        } catch (ExecutionException exception) {
            log.warn("Unable to check task reminder claim for user={}, task={}",
                    userId, claimedTask.getId(), exception);
            return false;
        }
    }

    public boolean finishTaskReminder(
            String userId,
            String taskId,
            String status,
            Instant now) {
        DocumentReference taskReference = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId);

        try {
            return firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(taskReference).get();
                if (!snapshot.exists()
                        || !"sending".equals(snapshot.getString("reminderStatus"))) {
                    return false;
                }

                var updates = new HashMap<String, Object>();
                updates.put("reminderStatus", status);
                updates.put("updatedAt", now.toString());
                if ("sent".equals(status)) {
                    updates.put("reminderSentAt", now.toString());
                }
                transaction.update(taskReference, updates);
                return true;
            }).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while finishing task reminder for user={}, task={}", userId, taskId, exception);
            return false;
        } catch (ExecutionException exception) {
            log.error("Unable to finish task reminder for user={}, task={}", userId, taskId, exception);
            return false;
        }
    }

    public UserSnapshot findUserSnapshot(String userId) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users")
                    .document(userId)
                    .get()
                    .get();

            if (!snapshot.exists()) {
                return null;
            }

            return UserSnapshot.builder()
                    .uid(snapshot.getString("uid"))
                    .displayName(snapshot.getString("displayName"))
                    .email(snapshot.getString("email"))
                    .photoURL(snapshot.getString("photoURL"))
                    .timezone(snapshot.getString("timezone"))
                    .createdAt(snapshot.get("createdAt"))
                    .lastLoginAt(snapshot.get("lastLoginAt"))
                    .habits(findHabitsByUserId(userId))
                    .deviceTokens(findDeviceTokensByUserId(userId))
                    .build();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.warn("Unable to retrieve user {} from Firestore; skipping", userId, e);
            return null;
        }
    }

    public void deleteDeviceToken(String uid, String token) {
        try {
            var staleTokens = firestore.collection("users")
                    .document(uid)
                    .collection("deviceTokens")
                    .whereEqualTo("token", token)
                    .get()
                    .get()
                    .getDocuments();
            if (staleTokens.isEmpty()) {
                return;
            }

            var batch = firestore.batch();
            staleTokens.forEach(document -> batch.delete(document.getReference()));
            batch.commit().get();
            log.info("Deleted stale device token for user={}", uid);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while deleting stale device token for user={}", uid, exception);
        } catch (ExecutionException exception) {
            log.error("Failed to delete stale device token for user={}", uid, exception);
        }
    }
}

