package com.mybill.MyBill_Backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseRequiredConfigTest {

    private final FirebaseRequiredConfig config = new FirebaseRequiredConfig();

    @Test
    void rejectsMissingFirebaseConfigInProd() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod");

        assertThatThrownBy(() -> config.requireFirebaseInProd(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIREBASE_CONFIG_JSON");
    }

    @Test
    void allowsFirebaseConfigInProd() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("firebase.config.json", "{\"type\":\"service_account\"}");

        assertThatCode(() -> config.requireFirebaseInProd(environment).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsMissingFirebaseConfigOutsideProd() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "dev");

        assertThatCode(() -> config.requireFirebaseInProd(environment).run(null))
                .doesNotThrowAnyException();
    }
}
