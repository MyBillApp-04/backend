package com.mybill.MyBill_Backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@Profile("!test")
@Lazy(false)
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final String FIREBASE_ENV_KEY = "FIREBASE_CONFIG_JSON";
    private static final String FIREBASE_PROPERTY_KEY = "firebase.config.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Environment environment;

    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initializeFirebase() {
        try {
            List<FirebaseApp> existingApps = FirebaseApp.getApps();

            if (!existingApps.isEmpty()) {
                logger.info("Firebase Admin SDK already initialized.");
                return;
            }

            InputStream serviceAccount = getFirebaseServiceAccount();

            initializeFirebaseApp(serviceAccount);
            logger.info("Firebase Admin SDK initialized successfully.");

        } catch (Exception exception) {
            logger.error("Firebase Admin SDK initialization failed: exception={} message={}",
                    exception.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(exception.getMessage()));
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", exception);
        }
    }

    InputStream getFirebaseServiceAccount() throws IOException {
        String firebaseConfigJson = firstNonBlank(
                System.getenv(FIREBASE_ENV_KEY),
                environment.getProperty(FIREBASE_PROPERTY_KEY),
                environment.getProperty(FIREBASE_ENV_KEY)
        );

        if (firebaseConfigJson != null && !firebaseConfigJson.trim().isEmpty()) {
            String normalizedJson = normalizeFirebaseJson(firebaseConfigJson);
            return new ByteArrayInputStream(normalizedJson.getBytes(StandardCharsets.UTF_8));
        }

        throw new IOException(
                "Firebase configuration missing. Set FIREBASE_CONFIG_JSON at runtime."
        );
    }

    private String normalizeFirebaseJson(String rawJson) throws IOException {
        String json = stripWrappingQuotes(rawJson.trim());

        if (isValidJson(json)) {
            return json;
        }

        json = json
                .replace("\\\"", "\"")
                .replace("\\r\\n", "\\n");

        if (isValidJson(json)) {
            return json;
        }

        throw new IOException("FIREBASE_CONFIG_JSON does not contain valid service account JSON.");
    }

    private boolean isValidJson(String json) {
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);

            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }

        return null;
    }

    private void initializeFirebaseApp(InputStream serviceAccount) throws IOException {
        try (InputStream inputStream = serviceAccount) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }
}
