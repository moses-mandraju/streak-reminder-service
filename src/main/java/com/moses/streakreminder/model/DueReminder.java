package com.moses.streakreminder.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DueReminder {
    private String userId;
    private String habitId;
    private String habitTitle;
    private String emoji;
    private String notificationTitle;
    private String notificationMessage;
    private String reminderTime;
    private String reminderTimezone;
    @Builder.Default
    private List<String> deviceTokens = List.of();
}
