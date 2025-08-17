package com.yourco.econdigest.batch;

import com.yourco.econdigest.batch.config.JobParameters;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ECON_DAILY_DIGEST Job 테스트
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class EconDailyDigestJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 Job Repository 정리
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    void testEconDailyDigestJobExecution() throws Exception {
        // Job 실행
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "3")
                .addString(JobParameters.DRY_RUN, "true")
                .addString(JobParameters.USE_LLM, "false")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Job 실행 결과 검증
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(5, jobExecution.getStepExecutions().size()); // 5개 Step이 모두 실행되었는지 확인

        // 각 Step 실행 상태 검증
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
            System.out.println("Step: " + stepExecution.getStepName() + 
                             " - Status: " + stepExecution.getStatus() + 
                             " - Duration: " + (stepExecution.getEndTime().getTime() - stepExecution.getStartTime().getTime()) + "ms");
        });
    }

    @Test
    void testJobWithValidParameters() throws Exception {
        // 유효한 파라미터로 Job 실행
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "2025-08-16")
                .addString(JobParameters.MAX_ARTICLES, "10")
                .addString(JobParameters.DRY_RUN, "false")
                .addString(JobParameters.USE_LLM, "true")
                .addString(JobParameters.FORCE_REFRESH, "true")
                .addString(JobParameters.LOG_LEVEL, "DEBUG")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
    }

    @Test
    void testJobWithInvalidParameters() {
        // 잘못된 파라미터로 Job 실행 시 예외가 발생해야 함
        org.springframework.batch.core.JobParameters jobParameters = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "200") // 유효 범위: 1-100
                .addString(JobParameters.DRY_RUN, "true")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 파라미터 검증 실패로 JobParametersInvalidException이 발생해야 함
        assertThrows(org.springframework.batch.core.JobParametersInvalidException.class, 
                () -> jobLauncherTestUtils.launchJob(jobParameters));
    }

    @Test
    void testIndividualSteps() throws Exception {
        // 개별 Step 테스트 - 각 Step마다 다른 timestamp 사용
        long baseTimestamp = System.currentTimeMillis();

        // S1_FETCH Step 테스트
        org.springframework.batch.core.JobParameters jobParameters1 = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "5")
                .addLong("timestamp", baseTimestamp + 1)
                .toJobParameters();
        JobExecution stepExecution = jobLauncherTestUtils.launchStep("S1_FETCH", jobParameters1);
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // S2_EXTRACT Step 테스트
        org.springframework.batch.core.JobParameters jobParameters2 = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "5")
                .addLong("timestamp", baseTimestamp + 2)
                .toJobParameters();
        stepExecution = jobLauncherTestUtils.launchStep("S2_EXTRACT", jobParameters2);
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // S3_SUMMARIZE_AI Step 테스트
        org.springframework.batch.core.JobParameters jobParameters3 = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "5")
                .addLong("timestamp", baseTimestamp + 3)
                .toJobParameters();
        stepExecution = jobLauncherTestUtils.launchStep("S3_SUMMARIZE_AI", jobParameters3);
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // S4_RANK_COMPOSE Step 테스트
        org.springframework.batch.core.JobParameters jobParameters4 = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "5")
                .addLong("timestamp", baseTimestamp + 4)
                .toJobParameters();
        stepExecution = jobLauncherTestUtils.launchStep("S4_RANK_COMPOSE", jobParameters4);
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // S5_DISPATCH Step 테스트
        org.springframework.batch.core.JobParameters jobParameters5 = new JobParametersBuilder()
                .addString(JobParameters.TARGET_DATE, "yesterday")
                .addString(JobParameters.MAX_ARTICLES, "5")
                .addLong("timestamp", baseTimestamp + 5)
                .toJobParameters();
        stepExecution = jobLauncherTestUtils.launchStep("S5_DISPATCH", jobParameters5);
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
    }
}