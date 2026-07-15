package com.mybill.MyBill_Backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FirebaseRequiredConfig {

    @Bean
    ApplicationRunner requireFirebaseInProd(Environment environment) {
        return args -> {
            if (!acceptsProfile(environment, "prod")) {
                return;
            }

            String firebaseConfigJson = firstNonBlank(
                    System.getenv("FIREBASE_CONFIG_JSON"),
                    environment.getProperty("firebase.config.json"),
                    environment.getProperty("FIREBASE_CONFIG_JSON")
            );
            if (firebaseConfigJson == null) {
                throw new IllegalStateException(
                        "Firebase auth is required in prod. Set FIREBASE_CONFIG_JSON to the Firebase service account JSON."
                );
            }
        };
    }

    private boolean acceptsProfile(Environment environment, String profile) {
        for (String activeProfile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
