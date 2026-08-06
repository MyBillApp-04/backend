package com.mybill.MyBill_Backend;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPropertiesTest {

    @Test
    void applicationPropertiesHasNoDuplicateKeys() throws Exception {
        assertNoDuplicateKeys("/application.properties");
    }

    @Test
    void applicationDevPropertiesHasNoDuplicateKeys() throws Exception {
        assertNoDuplicateKeys("/application-dev.properties");
    }

    @Test
    void applicationProdPropertiesHasNoDuplicateKeys() throws Exception {
        assertNoDuplicateKeys("/application-prod.properties");
    }

    private void assertNoDuplicateKeys(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertThat(is)
                    .withFailMessage("Properties file not found: " + resourcePath)
                    .isNotNull();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            Set<String> keys = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    boolean added = keys.add(key);
                    assertThat(added)
                            .withFailMessage("Duplicate key found in " + resourcePath + ": " + key)
                            .isTrue();
                }
            }
        }
    }
}
