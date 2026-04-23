package com.nutalig.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfiguration {
    @PostConstruct
    public void firebaseInitialization() throws IOException {
        try (InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("dpk-flower-firebase-adminsdk.json")) {

            if (serviceAccount == null) {
                throw new IllegalStateException("Firebase service account file not found in resources!");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }
}
