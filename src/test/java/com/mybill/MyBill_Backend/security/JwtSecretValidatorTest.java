package com.mybill.MyBill_Backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    @Test
    void acceptsSecureKeys() {
        String secureKey = "this-is-a-dummy-secret-key-for-testing-purposes-only-make-it-long-enough";
        assertThatCode(() -> JwtSecretValidator.validate(secureKey))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullOrEmptyKeys() {
        assertThatThrownBy(() -> JwtSecretValidator.validate(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing or empty");

        assertThatThrownBy(() -> JwtSecretValidator.validate("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing or empty");
    }

    @Test
    void rejectsUnresolvedPlaceholders() {
        assertThatThrownBy(() -> JwtSecretValidator.validate("some-prefix-${JWT_SECRET}-suffix"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unresolved configuration placeholder");
    }

    @Test
    void rejectsShortKeys() {
        String shortKey = "short-key-under-32-bytes";
        assertThatThrownBy(() -> JwtSecretValidator.validate(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload length is insufficient");
    }

    @Test
    void rejectsDictionaryWeakKeys() {
        assertThatThrownBy(() -> JwtSecretValidator.validate("12345678901234567890123456789012"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("common weak dictionary key");

        assertThatThrownBy(() -> JwtSecretValidator.validate("abcdefghijklmnopqrstuvwxyzabcdef"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("common weak dictionary key");
    }

    @Test
    void rejectsLowEntropyKeys() {
        // "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" is 32 bytes but only has 1 unique character
        assertThatThrownBy(() -> JwtSecretValidator.validate("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extremely low entropy");
    }
}
