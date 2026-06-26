package com.streakreminder.model;

import com.google.cloud.Timestamp;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceToken {
    private String token;
    private Timestamp createdAt;
    private Timestamp lastSeenAt;
}
