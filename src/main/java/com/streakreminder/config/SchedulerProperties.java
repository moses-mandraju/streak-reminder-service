package com.streakreminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {
    private boolean enabled = true;
    private int pollIntervalSeconds = 60;
}
