package com.yourco.econdigest.batch.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionContextUtil 테스트
 */
class ExecutionContextUtilTest {

    private StepExecution stepExecution;
    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        jobExecution = new JobExecution(1L);
        stepExecution = new StepExecution("testStep", jobExecution);
    }

    @Test
    void testPutAndGetFromJobContext() {
        // Job ExecutionContext에 값 저장 및 조회 테스트
        String key = "testKey";
        String value = "testValue";

        ExecutionContextUtil.putToJobContext(stepExecution, key, value);
        String retrieved = ExecutionContextUtil.getFromJobContext(stepExecution, key, String.class);

        assertEquals(value, retrieved);
    }

    @Test
    void testGetFromJobContextWithWrongType() {
        // 잘못된 타입으로 조회 시 null 반환 테스트
        String key = "testKey";
        String value = "testValue";

        ExecutionContextUtil.putToJobContext(stepExecution, key, value);
        Integer retrieved = ExecutionContextUtil.getFromJobContext(stepExecution, key, Integer.class);

        assertNull(retrieved);
    }

    @Test
    void testGetIntFromJobContext() {
        // Integer 값 저장 및 조회 테스트
        String key = ExecutionContextUtil.FETCHED_ARTICLES_COUNT;
        Integer value = 10;
        Integer defaultValue = 0;

        // 값이 존재하는 경우
        ExecutionContextUtil.putToJobContext(stepExecution, key, value);
        Integer retrieved = ExecutionContextUtil.getIntFromJobContext(stepExecution, key, defaultValue);
        assertEquals(value, retrieved);

        // 값이 존재하지 않는 경우 기본값 반환
        Integer notFound = ExecutionContextUtil.getIntFromJobContext(stepExecution, "nonExistentKey", defaultValue);
        assertEquals(defaultValue, notFound);
    }

    @Test
    void testGetStringFromJobContext() {
        // String 값 저장 및 조회 테스트
        String key = ExecutionContextUtil.DISPATCH_STATUS;
        String value = "SUCCESS";
        String defaultValue = "UNKNOWN";

        // 값이 존재하는 경우
        ExecutionContextUtil.putToJobContext(stepExecution, key, value);
        String retrieved = ExecutionContextUtil.getStringFromJobContext(stepExecution, key, defaultValue);
        assertEquals(value, retrieved);

        // 값이 존재하지 않는 경우 기본값 반환
        String notFound = ExecutionContextUtil.getStringFromJobContext(stepExecution, "nonExistentKey", defaultValue);
        assertEquals(defaultValue, notFound);
    }

    @Test
    void testGetLongFromJobContext() {
        // Long 값 저장 및 조회 테스트
        String key = ExecutionContextUtil.PROCESSING_START_TIME;
        Long value = System.currentTimeMillis();
        Long defaultValue = 0L;

        // 값이 존재하는 경우
        ExecutionContextUtil.putToJobContext(stepExecution, key, value);
        Long retrieved = ExecutionContextUtil.getLongFromJobContext(stepExecution, key, defaultValue);
        assertEquals(value, retrieved);

        // 값이 존재하지 않는 경우 기본값 반환
        Long notFound = ExecutionContextUtil.getLongFromJobContext(stepExecution, "nonExistentKey", defaultValue);
        assertEquals(defaultValue, notFound);
    }

    @Test
    void testPutAndGetFromStepContext() {
        // Step ExecutionContext에 값 저장 및 조회 테스트
        String key = "stepKey";
        String value = "stepValue";

        ExecutionContextUtil.putToStepContext(stepExecution, key, value);
        String retrieved = ExecutionContextUtil.getFromStepContext(stepExecution, key, String.class);

        assertEquals(value, retrieved);
    }

    @Test
    void testJobAndStepContextSeparation() {
        // Job Context와 Step Context가 분리되어 있는지 테스트
        String key = "sameKey";
        String jobValue = "jobValue";
        String stepValue = "stepValue";

        ExecutionContextUtil.putToJobContext(stepExecution, key, jobValue);
        ExecutionContextUtil.putToStepContext(stepExecution, key, stepValue);

        String retrievedFromJob = ExecutionContextUtil.getFromJobContext(stepExecution, key, String.class);
        String retrievedFromStep = ExecutionContextUtil.getFromStepContext(stepExecution, key, String.class);

        assertEquals(jobValue, retrievedFromJob);
        assertEquals(stepValue, retrievedFromStep);
        assertNotEquals(retrievedFromJob, retrievedFromStep);
    }

    @Test
    void testAllExecutionContextKeys() {
        // 모든 정의된 키 상수들이 사용 가능한지 테스트
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.FETCHED_ARTICLES_COUNT, 5);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT, 4);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT, 3);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.FINAL_DIGEST_ID, 12345L);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.DISPATCH_STATUS, "SUCCESS");
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.PROCESSING_START_TIME, 1000L);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.PROCESSING_END_TIME, 2000L);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.ERROR_COUNT, 0);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.WARNING_COUNT, 1);

        // 모든 값이 정상적으로 저장되고 조회되는지 확인
        assertEquals(Integer.valueOf(5), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.FETCHED_ARTICLES_COUNT, Integer.class));
        assertEquals(Integer.valueOf(4), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT, Integer.class));
        assertEquals(Integer.valueOf(3), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT, Integer.class));
        assertEquals(Long.valueOf(12345L), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.FINAL_DIGEST_ID, Long.class));
        assertEquals("SUCCESS", ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.DISPATCH_STATUS, String.class));
        assertEquals(Long.valueOf(1000L), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.PROCESSING_START_TIME, Long.class));
        assertEquals(Long.valueOf(2000L), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.PROCESSING_END_TIME, Long.class));
        assertEquals(Integer.valueOf(0), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.ERROR_COUNT, Integer.class));
        assertEquals(Integer.valueOf(1), ExecutionContextUtil.getFromJobContext(stepExecution, ExecutionContextUtil.WARNING_COUNT, Integer.class));
    }
}