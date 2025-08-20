package com.yourco.econyang.batch.util;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionContextUtil 단위 테스트
 */
class ExecutionContextUtilTest {

    @Test
    void testPutAndGetToJobContext() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String key = "testKey";
        String expectedValue = "testValue";

        // When
        ExecutionContextUtil.putToJobContext(stepExecution, key, expectedValue);
        String retrievedValue = ExecutionContextUtil.getFromJobContext(stepExecution, key, String.class);

        // Then
        assertNotNull(retrievedValue);
        assertEquals(expectedValue, retrievedValue);
    }

    @Test
    void testGetFromJobContext_NotFound() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);

        // When
        String result = ExecutionContextUtil.getFromJobContext(stepExecution, "nonexistent", String.class);

        // Then
        assertNull(result);
    }

    @Test
    void testPutAndGetIntFromJobContext() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String key = ExecutionContextUtil.FETCHED_ARTICLES_COUNT;
        Integer expectedCount = 42;

        // When
        ExecutionContextUtil.putToJobContext(stepExecution, key, expectedCount);
        Integer retrievedCount = ExecutionContextUtil.getIntFromJobContext(stepExecution, key, 0);

        // Then
        assertEquals(expectedCount, retrievedCount);
    }

    @Test
    void testGetIntFromJobContext_DefaultValue() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        Integer defaultValue = 10;

        // When
        Integer count = ExecutionContextUtil.getIntFromJobContext(stepExecution, "nonexistent", defaultValue);

        // Then
        assertEquals(defaultValue, count);
    }

    @Test
    void testStringFromJobContext() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String key = ExecutionContextUtil.DISPATCH_STATUS;
        String expectedValue = "COMPLETED";

        // When
        ExecutionContextUtil.putToJobContext(stepExecution, key, expectedValue);
        String retrievedValue = ExecutionContextUtil.getStringFromJobContext(stepExecution, key, "DEFAULT");

        // Then
        assertEquals(expectedValue, retrievedValue);
    }

    @Test
    void testGetStringFromJobContext_DefaultValue() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String defaultValue = "DEFAULT_STATUS";

        // When
        String status = ExecutionContextUtil.getStringFromJobContext(stepExecution, "nonexistent", defaultValue);

        // Then
        assertEquals(defaultValue, status);
    }

    @Test
    void testLongFromJobContext() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String key = ExecutionContextUtil.FINAL_DIGEST_ID;
        Long expectedValue = 123L;

        // When
        ExecutionContextUtil.putToJobContext(stepExecution, key, expectedValue);
        Long retrievedValue = ExecutionContextUtil.getLongFromJobContext(stepExecution, key, 0L);

        // Then
        assertEquals(expectedValue, retrievedValue);
    }

    @Test
    void testPutAndGetToStepContext() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        String key = "stepKey";
        String expectedValue = "stepValue";

        // When
        ExecutionContextUtil.putToStepContext(stepExecution, key, expectedValue);
        String retrievedValue = ExecutionContextUtil.getFromStepContext(stepExecution, key, String.class);

        // Then
        assertNotNull(retrievedValue);
        assertEquals(expectedValue, retrievedValue);
    }

    @Test
    void testGetFromStepContext_NotFound() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);

        // When
        String result = ExecutionContextUtil.getFromStepContext(stepExecution, "nonexistent", String.class);

        // Then
        assertNull(result);
    }

    @Test
    void testContextConstants() {
        // Given & When & Then - Context key 상수들이 정의되어 있는지 확인
        assertNotNull(ExecutionContextUtil.FETCHED_ARTICLES_COUNT);
        assertNotNull(ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT);
        assertNotNull(ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT);
        assertNotNull(ExecutionContextUtil.FINAL_DIGEST_ID);
        assertNotNull(ExecutionContextUtil.DISPATCH_STATUS);
        assertNotNull(ExecutionContextUtil.PROCESSING_START_TIME);
        assertNotNull(ExecutionContextUtil.PROCESSING_END_TIME);
        assertNotNull(ExecutionContextUtil.ERROR_COUNT);
        assertNotNull(ExecutionContextUtil.WARNING_COUNT);
    }

    @Test
    void testComplexScenario() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        
        // When - 여러 데이터를 순차적으로 저장
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.FETCHED_ARTICLES_COUNT, 10);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT, 8);
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.DISPATCH_STATUS, "PROCESSING");
        ExecutionContextUtil.putToJobContext(stepExecution, ExecutionContextUtil.ERROR_COUNT, 2);
        ExecutionContextUtil.putToStepContext(stepExecution, "stepData", "stepInfo");

        // Then - 모든 데이터가 올바르게 저장되고 조회됨
        assertEquals(Integer.valueOf(10), ExecutionContextUtil.getIntFromJobContext(stepExecution, ExecutionContextUtil.FETCHED_ARTICLES_COUNT, 0));
        assertEquals(Integer.valueOf(8), ExecutionContextUtil.getIntFromJobContext(stepExecution, ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT, 0));
        assertEquals("PROCESSING", ExecutionContextUtil.getStringFromJobContext(stepExecution, ExecutionContextUtil.DISPATCH_STATUS, "UNKNOWN"));
        assertEquals(Integer.valueOf(2), ExecutionContextUtil.getIntFromJobContext(stepExecution, ExecutionContextUtil.ERROR_COUNT, 0));
        assertEquals("stepInfo", ExecutionContextUtil.getFromStepContext(stepExecution, "stepData", String.class));
    }

    @Test
    void testDifferentDataTypes() {
        // Given
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);

        // When & Then - 다양한 데이터 타입 테스트
        ExecutionContextUtil.putToJobContext(stepExecution, "intValue", 42);
        ExecutionContextUtil.putToJobContext(stepExecution, "longValue", 123L);
        ExecutionContextUtil.putToJobContext(stepExecution, "stringValue", "test");
        ExecutionContextUtil.putToJobContext(stepExecution, "booleanValue", true);

        assertEquals(Integer.valueOf(42), ExecutionContextUtil.getFromJobContext(stepExecution, "intValue", Integer.class));
        assertEquals(Long.valueOf(123L), ExecutionContextUtil.getFromJobContext(stepExecution, "longValue", Long.class));
        assertEquals("test", ExecutionContextUtil.getFromJobContext(stepExecution, "stringValue", String.class));
        assertEquals(Boolean.TRUE, ExecutionContextUtil.getFromJobContext(stepExecution, "booleanValue", Boolean.class));
    }

    @Test
    void testUtilityClassInstantiation() throws Exception {
        // When & Then - Utility 클래스는 인스턴스화할 수 없어야 함
        java.lang.reflect.Constructor<?> constructor = ExecutionContextUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        // 실제 UnsupportedOperationException이 원인인지 확인
        try {
            constructor.newInstance();
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            assertEquals("Utility class", e.getCause().getMessage());
        }
    }
}