package com.yourco.econdigest.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EconDigestJobParametersValidator 단위 테스트
 */
class EconDigestJobParametersValidatorTest {

    private EconDigestJobParametersValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EconDigestJobParametersValidator();
    }

    @Test
    void testValidate_ValidParameters() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 10L)
                .addString("dryRun", "false")
                .addString("useLLM", "true")
                .addString("forceRefresh", "false")
                .addString("logLevel", "INFO")
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(jobParameters));
    }

    @Test
    void testValidate_MissingTargetDate() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("maxArticles", 10L)
                .addString("dryRun", "false")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("targetDate"));
    }

    @Test
    void testValidate_InvalidTargetDate() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "invalid-date")
                .addLong("maxArticles", 10L)
                .addString("dryRun", "false")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("targetDate"));
    }

    @Test
    void testValidate_InvalidMaxArticles() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", -1L)
                .addString("dryRun", "false")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("maxArticles"));
    }

    @Test
    void testValidate_InvalidBooleanParameters() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 10L)
                .addString("dryRun", "invalid-boolean")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("dryRun"));
    }

    @Test
    void testValidate_InvalidLogLevel() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 10L)
                .addString("dryRun", "false")
                .addString("logLevel", "INVALID")
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("logLevel"));
    }

    @Test
    void testValidate_DefaultValues() {
        // Given - 최소한의 파라미터만 제공
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .toJobParameters();

        // When & Then - 기본값으로 유효성 검증 통과해야 함
        assertDoesNotThrow(() -> validator.validate(jobParameters));
    }

    @Test
    void testValidate_EdgeCaseMaxArticles() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 1000L) // 큰 값
                .addString("dryRun", "true")
                .toJobParameters();

        // When & Then
        assertDoesNotThrow(() -> validator.validate(jobParameters));
    }

    @Test
    void testValidate_FutureDate() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(1);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", futureDate.toString())
                .addLong("maxArticles", 10L)
                .toJobParameters();

        // When & Then
        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("future"));
    }

    @Test
    void testValidate_AllValidLogLevels() {
        // Given
        String[] validLogLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
        
        for (String logLevel : validLogLevels) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", LocalDate.now().toString())
                    .addString("logLevel", logLevel)
                    .toJobParameters();

            // When & Then
            assertDoesNotThrow(() -> validator.validate(jobParameters),
                    "LogLevel " + logLevel + " should be valid");
        }
    }
}