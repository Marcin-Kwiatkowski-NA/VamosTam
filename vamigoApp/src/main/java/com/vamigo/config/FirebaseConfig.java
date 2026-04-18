package com.vamigo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials-path:}")
    private String firebaseCredentialsPath;

    @Value("${fcm.credentials-path:}")
    private String fcmCredentialsPath;

    @PostConstruct
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            String credentialsPath = resolveCredentialsPath();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized with credentials from {}", credentialsPath);
        }
    }

    private String resolveCredentialsPath() {
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank()) {
            return firebaseCredentialsPath;
        }
        if (fcmCredentialsPath != null && !fcmCredentialsPath.isBlank()) {
            log.warn("fcm.credentials-path is deprecated; use firebase.credentials-path");
            return fcmCredentialsPath;
        }
        throw new IllegalStateException(
                "Firebase credentials path is not configured. Set firebase.credentials-path.");
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}
