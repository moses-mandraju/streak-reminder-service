package com.moses.streakreminder.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.moses.streakreminder.model.DeviceToken;
import com.moses.streakreminder.model.Habit;
import com.moses.streakreminder.model.UserSnapshot;
import com.moses.streakreminder.util.FirestoreMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FirestoreRepository {

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
        throw new IllegalStateException("Unable to retrieve user ids", e);
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
            throw new IllegalStateException("Unable to retrieve habits for user " + userId, e);
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
            throw new IllegalStateException("Unable to retrieve device tokens for user " + userId, e);
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
        throw new IllegalStateException("Unable to retrieve user " + userId, e);
    }
}

   public void deleteDeviceToken(String token) {
    try {
        Firestore db = FirestoreClient.getFirestore();

        // Query across all users' deviceTokens subcollections
        db.collectionGroup("deviceTokens")
          .whereEqualTo("token", token)
          .get()
          .get() // blocking call
          .getDocuments()
          .forEach(doc -> {
              doc.getReference().delete();
              log.info("Deleted stale token doc: {}", doc.getId());
          });

    } catch (Exception ex) {
        log.error("Failed to delete stale token: {}", token, ex);
        // Don't rethrow — stale token cleanup is best-effort
    }
}
}

