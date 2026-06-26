package com.moses.streakreminder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {
    private String projectId;
    private String serviceAccountFilePath;
}

