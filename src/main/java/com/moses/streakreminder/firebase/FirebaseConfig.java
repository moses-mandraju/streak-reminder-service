package com.moses.streakreminder.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.moses.streakreminder.config.FirebaseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;
    private final ResourceLoader resourceLoader;

    public FirebaseConfig(FirebaseProperties firebaseProperties,
                          ResourceLoader resourceLoader) {
        this.firebaseProperties = firebaseProperties;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp already initialized.");
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;

        // Render / Production
        String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");

        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {

            credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(
                            serviceAccountJson.getBytes(StandardCharsets.UTF_8)));

            log.info("Loaded Firebase credentials from environment variable.");

        } else {

            // Local Development
            Resource serviceAccountResource =
                    resourceLoader.getResource(firebaseProperties.getServiceAccountFilePath());

            try (InputStream credentialsStream = serviceAccountResource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(credentialsStream);
            }

            log.info("Loaded Firebase credentials from local service account file.");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(firebaseProperties.getProjectId())
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);

        log.info("Initialized FirebaseApp for project {}", firebaseProperties.getProjectId());

        return app;
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}