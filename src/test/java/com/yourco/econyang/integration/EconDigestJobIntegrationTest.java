package com.yourco.econyang.integration;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ECON_DAILY_DIGEST Job 통합 테스트
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class EconDigestJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    void testJobExecution_BasicScenario() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().minusDays(1).toString())
                .addLong("maxArticles", 5L)
                .addString("dryRun", "true")
                .addString("useLLM", "false") // LLM 호출 비활성화
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    void testJobExecution_DryRun() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 3L)
                .addString("dryRun", "true")
                .addString("forceRefresh", "true")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // DryRun이므로 실제 처리는 하지 않음
        assertTrue(jobExecution.getExecutionContext().containsKey("dryRun"));
    }

    @Test
    void testStepExecution_S1_FETCH() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 5L)
                .addString("dryRun", "true")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("S1_FETCH", jobParameters);

        // Then
        assertNotNull(jobExecution);
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals("S1_FETCH", stepExecution.getStepName());
        assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
    }

    @Test
    void testStepExecution_S2_EXTRACT() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 3L)
                .addString("dryRun", "true")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("S2_EXTRACT", jobParameters);

        // Then
        assertNotNull(jobExecution);
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals("S2_EXTRACT", stepExecution.getStepName());
        assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
    }

    @Test
    void testStepExecution_S3_SUMMARIZE_AI() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 2L)
                .addString("dryRun", "true")
                .addString("useLLM", "false") // LLM 호출 비활성화
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("S3_SUMMARIZE_AI", jobParameters);

        // Then
        assertNotNull(jobExecution);
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals("S3_SUMMARIZE_AI", stepExecution.getStepName());
        assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
    }

    @Test
    void testJobParameters_Validation() {
        // Given - 잘못된 파라미터
        JobParameters invalidJobParameters = new JobParametersBuilder()
                .addString("targetDate", "invalid-date")
                .addLong("maxArticles", -1L)
                .toJobParameters();

        // When & Then
        assertThrows(Exception.class, () -> {
            jobLauncherTestUtils.launchJob(invalidJobParameters);
        });
    }

    @Test
    void testJobExecution_MinimalParameters() throws Exception {
        // Given - 최소한의 파라미터
        JobParameters minimalParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().minusDays(1).toString())
                .addString("dryRun", "true")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(minimalParameters);

        // Then
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
    }

    @Test
    void testJobExecution_LogLevel() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addString("dryRun", "true")
                .addString("logLevel", "DEBUG")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertTrue(jobExecution.getJobParameters().getString("logLevel").equals("DEBUG"));
    }

    @Test
    void testStepSequence() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addLong("maxArticles", 2L)
                .addString("dryRun", "true")
                .addString("useLLM", "false")
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // Step 실행 순서 확인
        String[] expectedSteps = {"S1_FETCH", "S2_EXTRACT", "S3_SUMMARIZE_AI", "S4_RANK_COMPOSE", "S5_DISPATCH"};
        int stepIndex = 0;
        
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            assertTrue(stepIndex < expectedSteps.length);
            assertEquals(expectedSteps[stepIndex], stepExecution.getStepName());
            stepIndex++;
        }
        
        assertEquals(expectedSteps.length, stepIndex);
    }

    @Test
    void testJobRestart() throws Exception {
        // Given - 첫 번째 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addString("dryRun", "true")
                .addLong("timestamp", System.currentTimeMillis()) // 고유성을 위한 타임스탬프
                .toJobParameters();

        // When - 첫 번째 실행
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertEquals(BatchStatus.COMPLETED, firstExecution.getStatus());

        // When - 같은 파라미터로 재실행 (이미 완료된 Job은 재실행되지 않음)
        JobExecution secondExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then - 동일한 Job Instance
        assertEquals(firstExecution.getJobInstance().getId(), secondExecution.getJobInstance().getId());
    }
}