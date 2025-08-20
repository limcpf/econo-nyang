package com.yourco.econyang.batch.util;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Spring Batch ExecutionContext를 활용한 Step 간 데이터 전달 유틸리티
 */
public final class ExecutionContextUtil {
    
    // ExecutionContext Key 상수
    public static final String FETCHED_ARTICLES_COUNT = "fetchedArticlesCount";
    public static final String EXTRACTED_ARTICLES_COUNT = "extractedArticlesCount";
    public static final String SUMMARIZED_ARTICLES_COUNT = "summarizedArticlesCount";
    public static final String FINAL_DIGEST_ID = "finalDigestId";
    public static final String DISPATCH_STATUS = "dispatchStatus";
    public static final String PROCESSING_START_TIME = "processingStartTime";
    public static final String PROCESSING_END_TIME = "processingEndTime";
    public static final String ERROR_COUNT = "errorCount";
    public static final String WARNING_COUNT = "warningCount";
    
    /**
     * Job ExecutionContext에 값 저장
     */
    public static void putToJobContext(StepExecution stepExecution, String key, Object value) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        jobContext.put(key, value);
    }
    
    /**
     * Job ExecutionContext에서 값 조회
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFromJobContext(StepExecution stepExecution, String key, Class<T> type) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        Object value = jobContext.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Job ExecutionContext에서 Integer 값 조회 (기본값 포함)
     */
    public static Integer getIntFromJobContext(StepExecution stepExecution, String key, Integer defaultValue) {
        Integer value = getFromJobContext(stepExecution, key, Integer.class);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Job ExecutionContext에서 String 값 조회 (기본값 포함)
     */
    public static String getStringFromJobContext(StepExecution stepExecution, String key, String defaultValue) {
        String value = getFromJobContext(stepExecution, key, String.class);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Job ExecutionContext에서 Long 값 조회 (기본값 포함)
     */
    public static Long getLongFromJobContext(StepExecution stepExecution, String key, Long defaultValue) {
        Long value = getFromJobContext(stepExecution, key, Long.class);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Step ExecutionContext에 값 저장
     */
    public static void putToStepContext(StepExecution stepExecution, String key, Object value) {
        ExecutionContext stepContext = stepExecution.getExecutionContext();
        stepContext.put(key, value);
    }
    
    /**
     * Step ExecutionContext에서 값 조회
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFromStepContext(StepExecution stepExecution, String key, Class<T> type) {
        ExecutionContext stepContext = stepExecution.getExecutionContext();
        Object value = stepContext.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    // private constructor to prevent instantiation
    private ExecutionContextUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}