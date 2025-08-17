package com.yourco.econdigest.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.clean-disabled=false"
})
class FlywayMigrationTest {

    @Test
    void testFlywayMigrationSuccess() {
        // This test verifies that all Flyway migrations can be applied successfully
        // The Spring Boot test context will automatically run migrations during startup
        
        assertDoesNotThrow(() -> {
            // If we reach this point, migrations were successful
            System.out.println("All Flyway migrations completed successfully");
        });
    }
    
    @Test
    void testFlywayInfo() {
        // Additional test to verify migration metadata
        assertDoesNotThrow(() -> {
            // Create Flyway instance for testing
            Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:testdb", "sa", "")
                .locations("classpath:db/test-migration")
                .load();
            
            // Apply migrations
            flyway.migrate();
            
            // Verify migration info
            org.flywaydb.core.api.MigrationInfoService info = flyway.info();
            org.flywaydb.core.api.MigrationInfo[] migrations = info.all();
            
            System.out.println("Applied migrations:");
            for (org.flywaydb.core.api.MigrationInfo migration : migrations) {
                System.out.println("- " + migration.getVersion() + ": " + migration.getDescription());
            }
            
            // Should have at least 7 migrations (V1-V7)
            assert migrations.length >= 7;
        });
    }
}