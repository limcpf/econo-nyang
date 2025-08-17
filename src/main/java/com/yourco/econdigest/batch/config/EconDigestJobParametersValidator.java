package com.yourco.econdigest.batch.config;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ECON_DAILY_DIGEST Job의 파라미터 유효성 검증 클래스
 */
@Component
public class EconDigestJobParametersValidator implements JobParametersValidator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public void validate(org.springframework.batch.core.JobParameters parameters) 
            throws JobParametersInvalidException {
        
        // targetDate 검증
        String targetDate = parameters.getString(JobParameters.TARGET_DATE);
        if (targetDate != null && !targetDate.equals("yesterday") && !targetDate.equals("today")) {
            try {
                LocalDate.parse(targetDate, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new JobParametersInvalidException(
                    "targetDate must be 'yesterday', 'today', or valid date format (yyyy-MM-dd). Got: " + targetDate);
            }
        }
        
        // maxArticles 검증
        String maxArticlesStr = parameters.getString(JobParameters.MAX_ARTICLES);
        if (maxArticlesStr != null) {
            try {
                int maxArticles = Integer.parseInt(maxArticlesStr);
                if (maxArticles < 1 || maxArticles > 100) {
                    throw new JobParametersInvalidException(
                        "maxArticles must be between 1 and 100. Got: " + maxArticles);
                }
            } catch (NumberFormatException e) {
                throw new JobParametersInvalidException(
                    "maxArticles must be a valid integer. Got: " + maxArticlesStr);
            }
        }
        
        // dryRun 검증
        String dryRunStr = parameters.getString(JobParameters.DRY_RUN);
        if (dryRunStr != null && !dryRunStr.equals("true") && !dryRunStr.equals("false")) {
            throw new JobParametersInvalidException(
                "dryRun must be 'true' or 'false'. Got: " + dryRunStr);
        }
        
        // useLLM 검증
        String useLLMStr = parameters.getString(JobParameters.USE_LLM);
        if (useLLMStr != null && !useLLMStr.equals("true") && !useLLMStr.equals("false")) {
            throw new JobParametersInvalidException(
                "useLLM must be 'true' or 'false'. Got: " + useLLMStr);
        }
        
        // forceRefresh 검증
        String forceRefreshStr = parameters.getString(JobParameters.FORCE_REFRESH);
        if (forceRefreshStr != null && !forceRefreshStr.equals("true") && !forceRefreshStr.equals("false")) {
            throw new JobParametersInvalidException(
                "forceRefresh must be 'true' or 'false'. Got: " + forceRefreshStr);
        }
        
        // logLevel 검증
        String logLevel = parameters.getString(JobParameters.LOG_LEVEL);
        if (logLevel != null) {
            String[] validLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
            boolean isValid = false;
            for (String validLevel : validLevels) {
                if (validLevel.equals(logLevel.toUpperCase())) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new JobParametersInvalidException(
                    "logLevel must be one of: TRACE, DEBUG, INFO, WARN, ERROR. Got: " + logLevel);
            }
        }
    }
}