package com.moses.streakreminder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshot {

    private String uid;

    private String displayName;

    private String email;

    private String photoURL;

    private String timezone;

    private Object createdAt;

    private Object lastLoginAt;

    private List<Habit> habits;

    private List<DeviceToken> deviceTokens;

}