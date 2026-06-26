package com.streakreminder.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Habit {
    private String id;
    private String title;
    private String emoji;
    private String createdDate;
    private int currentStreak;
    private int longestStreak;
    private String lastCompletedDate;
    @Singular("completionHistoryItem")
    private List<String> completionHistory;
    private boolean reminderEnabled;
    private String reminderTime;
    private String reminderFrequency;
    @Singular("selectedDay")
    private List<String> selectedDays;
    private String notificationTitle;
    private String notificationMessage;
    private String reminderTimezone;
}
