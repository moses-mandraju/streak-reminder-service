package com.moses.streakreminder.util;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.moses.streakreminder.model.DeviceToken;
import com.moses.streakreminder.model.Habit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FirestoreMapper {

    private FirestoreMapper() {
    }

    public static Habit toHabit(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        return Habit.builder()
                .id(document.getId())
                .title(document.getString("title"))
                .emoji(document.getString("emoji"))
                .createdDate(document.getString("createdDate"))
                .currentStreak(getInteger(document, "currentStreak"))
                .longestStreak(getInteger(document, "longestStreak"))
                .lastCompletedDate(document.getString("lastCompletedDate"))
                .completionHistory(getStringList(document, "completionHistory"))
                .reminderEnabled(Boolean.TRUE.equals(document.getBoolean("reminderEnabled")))
                .reminderTime(document.getString("reminderTime"))
                .reminderFrequency(document.getString("reminderFrequency"))
                .selectedDays(getStringList(document, "selectedDays"))
                .notificationTitle(document.getString("notificationTitle"))
                .notificationMessage(document.getString("notificationMessage"))
                .reminderTimezone(document.getString("reminderTimezone"))
                .build();
    }

    public static DeviceToken toDeviceToken(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        return DeviceToken.builder()
                .token(document.getString("token"))
                .createdAt(document.contains("createdAt") ? document.getTimestamp("createdAt") : null)
                .lastSeenAt(document.contains("lastSeenAt") ? document.getTimestamp("lastSeenAt") : null)
                .build();
    }

    private static int getInteger(DocumentSnapshot document, String fieldName) {
        Object value = document.get(fieldName);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(DocumentSnapshot document, String fieldName) {
        Object raw = document.get(fieldName);
        if (raw instanceof List<?>) {
            List<?> rawList = (List<?>) raw;
            List<String> values = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
            return values;
        }
        return List.of();
    }
}

