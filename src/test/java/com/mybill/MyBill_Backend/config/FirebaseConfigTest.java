package com.mybill.MyBill_Backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseConfigTest {

    @Test
    void rejectsMissingRuntimeConfiguration() {
        FirebaseConfig config = new FirebaseConfig(new MockEnvironment());

        assertThatThrownBy(config::getFirebaseServiceAccount)
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("FIREBASE_CONFIG_JSON");
    }

    @Test
    void readsServiceAccountFromRuntimeProperty() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("firebase.config.json", "{\"type\":\"service_account\"}");
        FirebaseConfig config = new FirebaseConfig(environment);

        try (InputStream stream = config.getFirebaseServiceAccount()) {
            assertThat(stream).isNotNull();
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("{\"type\":\"service_account\"}");
        }
    }
}
