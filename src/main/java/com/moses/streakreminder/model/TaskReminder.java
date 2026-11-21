package com.moses.streakreminder.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TaskReminder {
    private String userId;
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String startTime;
    private boolean allDay;
    private boolean completed;
    private boolean carryForward;
    private boolean notificationEnabled;
    private int reminderMinutesBefore;
    private String reminderStatus;
    private String timezone;
}
