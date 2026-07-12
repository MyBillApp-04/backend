package com.mybill.MyBill_Backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:migration_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabaseMigrationIdempotencyTest {

    static {
        MigrationPreprocessor.process();
    }

    @Autowired
    private DataSource dataSource;

    private Flyway flyway;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN is_deleted SET DEFAULT false");
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
        jdbc.execute("ALTER TABLE customer_notification_templates ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP");
        jdbc.execute("ALTER TABLE email_templates ALTER COLUMN is_deleted SET DEFAULT false");

        flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .placeholders(Collections.singletonMap("createExtensionCommand", "SELECT 1"))
                .validateOnMigrate(false)
                .load();
    }

    @Test
    void verifyMigrationIdempotency() {
        // Run migration manually on the Hibernate-created schema
        var migrateResult1 = flyway.migrate();
        assertThat(migrateResult1.success).isTrue();

        // Run migrate again to verify it is idempotent (0 migrations executed)
        var migrateResult2 = flyway.migrate();
        assertThat(migrateResult2.migrationsExecuted).isZero();

        // Run migrate a third time to verify it remains idempotent
        var migrateResult3 = flyway.migrate();
        assertThat(migrateResult3.migrationsExecuted).isZero();
    }
}
