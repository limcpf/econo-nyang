package com.yourco.econdigest.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EconDigestJobParametersValidator 테스트
 */
class EconDigestJobParametersValidatorTest {

    private EconDigestJobParametersValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EconDigestJobParametersValidator();
    }

    @Test
    void testValidParameters() throws JobParametersInvalidException {
        // 유효한 파라미터들로 테스트
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "10")
                .addString(JobParameters.DRY_RUN, "true")
                .addString(JobParameters.USE_LLM, "false")
                .addString(JobParameters.FORCE_REFRESH, "true")
                .addString(JobParameters.LOG_LEVEL, "INFO")
                .toJobParameters();

        // 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> validator.validate(jobParameters));
    }

    @Test
    void testValidDateFormats() throws JobParametersInvalidException {
        // 유효한 날짜 형식들 테스트
        String[] validDates = {"yesterday", "today", "2025-08-17", "2025-01-01", "2025-12-31"};
        
        for (String date : validDates) {
            org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                    .addString(JobParameters.TARGET_DATE, date)
                    .toJobParameters();

            assertDoesNotThrow(() -> validator.validate(jobParameters), 
                "Valid date should not throw exception: " + date);
        }
    }

    @Test
    void testInvalidDateFormat() {
        // 잘못된 날짜 형식 테스트
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "2025/08/17") // 잘못된 형식
                .toJobParameters();

        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("targetDate must be"));
    }

    @Test
    void testMaxArticlesValidRange() throws JobParametersInvalidException {
        // 유효한 maxArticles 범위 테스트
        int[] validCounts = {1, 10, 50, 100};
        
        for (int count : validCounts) {
            org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                    .addString(JobParameters.MAX_ARTICLES, String.valueOf(count))
                    .toJobParameters();

            assertDoesNotThrow(() -> validator.validate(jobParameters),
                "Valid maxArticles should not throw exception: " + count);
        }
    }

    @Test
    void testMaxArticlesInvalidRange() {
        // 범위를 벗어난 maxArticles 테스트
        int[] invalidCounts = {0, -1, 101, 1000};
        
        for (int count : invalidCounts) {
            org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                    .addString(JobParameters.MAX_ARTICLES, String.valueOf(count))
                    .toJobParameters();

            JobParametersInvalidException exception = assertThrows(
                    JobParametersInvalidException.class,
                    () -> validator.validate(jobParameters)
            );
            assertTrue(exception.getMessage().contains("maxArticles must be between 1 and 100"));
        }
    }

    @Test
    void testMaxArticlesInvalidFormat() {
        // 숫자가 아닌 maxArticles 테스트
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.MAX_ARTICLES, "abc")
                .toJobParameters();

        JobParametersInvalidException exception = assertThrows(
                JobParametersInvalidException.class,
                () -> validator.validate(jobParameters)
        );
        assertTrue(exception.getMessage().contains("maxArticles must be a valid integer"));
    }

    @Test
    void testBooleanParametersValidValues() throws JobParametersInvalidException {
        // 유효한 boolean 값들 테스트
        String[] booleanParams = {JobParameters.DRY_RUN, JobParameters.USE_LLM, JobParameters.FORCE_REFRESH};
        String[] validValues = {"true", "false"};
        
        for (String param : booleanParams) {
            for (String value : validValues) {
                org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                        .addString(param, value)
                        .toJobParameters();

                assertDoesNotThrow(() -> validator.validate(jobParameters),
                    "Valid boolean parameter should not throw exception: " + param + "=" + value);
            }
        }
    }

    @Test
    void testBooleanParametersInvalidValues() {
        // 잘못된 boolean 값들 테스트
        String[] booleanParams = {JobParameters.DRY_RUN, JobParameters.USE_LLM, JobParameters.FORCE_REFRESH};
        String[] invalidValues = {"yes", "no", "1", "0", "True", "False"};
        
        for (String param : booleanParams) {
            for (String value : invalidValues) {
                org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                        .addString(param, value)
                        .toJobParameters();

                assertThrows(JobParametersInvalidException.class,
                    () -> validator.validate(jobParameters),
                    "Invalid boolean value should throw exception: " + param + "=" + value);
            }
        }
    }

    @Test
    void testLogLevelValidValues() throws JobParametersInvalidException {
        // 유효한 로그 레벨들 테스트
        String[] validLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "trace", "debug", "info", "warn", "error"};
        
        for (String level : validLevels) {
            org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                    .addString(JobParameters.LOG_LEVEL, level)
                    .toJobParameters();

            assertDoesNotThrow(() -> validator.validate(jobParameters),
                "Valid log level should not throw exception: " + level);
        }
    }

    @Test
    void testLogLevelInvalidValues() {
        // 잘못된 로그 레벨들 테스트
        String[] invalidLevels = {"VERBOSE", "ALL", "OFF", "FATAL", "invalid"};
        
        for (String level : invalidLevels) {
            org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                    .addString(JobParameters.LOG_LEVEL, level)
                    .toJobParameters();

            JobParametersInvalidException exception = assertThrows(
                    JobParametersInvalidException.class,
                    () -> validator.validate(jobParameters)
            );
            assertTrue(exception.getMessage().contains("logLevel must be one of"));
        }
    }

    @Test
    void testNullParametersAreValid() throws JobParametersInvalidException {
        // null 파라미터들은 기본값을 사용하므로 유효해야 함
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .toJobParameters();

        assertDoesNotThrow(() -> validator.validate(jobParameters));
    }
}