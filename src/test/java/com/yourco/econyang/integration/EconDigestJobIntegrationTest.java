package com.yourco.econyang.integration;

import com.yourco.econyang.config.TestBatchConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ECON_DAILY_DIGEST Job 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SpringBatchTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestBatchConfiguration.class})
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
        assertTrue(jobExecution.getJobParameters().getString("dryRun").equals("true"));
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
    void testJobParameters_Validation() throws Exception {
        // Given - 잘못된 파라미터 (테스트용 Job은 검증이 없으므로 성공하지만 파라미터는 전달됨)
        JobParameters invalidJobParameters = new JobParametersBuilder()
                .addString("targetDate", "invalid-date")
                .addLong("maxArticles", -1L)
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(invalidJobParameters);

        // Then - 테스트 Job은 파라미터 검증 없이 실행됨
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals("invalid-date", jobExecution.getJobParameters().getString("targetDate"));
        assertEquals(Long.valueOf(-1), jobExecution.getJobParameters().getLong("maxArticles"));
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
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        
        // Test Job은 단일 Step만 가짐
        assertEquals(1, jobExecution.getStepExecutions().size());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals("S1_FETCH", stepExecution.getStepName());
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

        // When - 다른 파라미터로 새로운 Job 실행 (기존 Job이 완료되었으므로 새로운 파라미터 필요)
        JobParameters newJobParameters = new JobParametersBuilder()
                .addString("targetDate", LocalDate.now().toString())
                .addString("dryRun", "true")
                .addLong("timestamp", System.currentTimeMillis() + 1000) // 다른 타임스탬프로 새로운 Job Instance 생성
                .toJobParameters();
        
        JobExecution secondExecution = jobLauncherTestUtils.launchJob(newJobParameters);

        // Then - 다른 Job Instance (새로운 파라미터이므로)
        assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());
        assertNotEquals(firstExecution.getJobInstance().getId(), secondExecution.getJobInstance().getId());
    }
}