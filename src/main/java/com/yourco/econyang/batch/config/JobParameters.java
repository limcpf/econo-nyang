package com.yourco.econyang.batch.config;

/**
 * ECON_DAILY_DIGEST Job에서 사용하는 파라미터 상수 정의
 */
public final class JobParameters {
    
    // Job Parameter 키 상수
    public static final String TARGET_DATE = "targetDate";
    public static final String MAX_ARTICLES = "maxArticles";
    public static final String DRY_RUN = "dryRun";
    public static final String USE_LLM = "useLLM";
    public static final String FORCE_REFRESH = "forceRefresh";
    public static final String LOG_LEVEL = "logLevel";
    
    // 기본값 상수
    public static final String DEFAULT_TARGET_DATE = "yesterday";
    public static final int DEFAULT_MAX_ARTICLES = 10;
    public static final boolean DEFAULT_DRY_RUN = false;
    public static final boolean DEFAULT_USE_LLM = true;
    public static final boolean DEFAULT_FORCE_REFRESH = false;
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    
    // private constructor to prevent instantiation
    private JobParameters() {
        throw new UnsupportedOperationException("Utility class");
    }
}