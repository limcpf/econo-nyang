package com.yourco.econyang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * RSS 소스 설정을 로드하는 Configuration 클래스
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "rss")
public class RssSourcesConfig {

    private List<RssSource> sources = new ArrayList<>();
    private FilterConfig filters = new FilterConfig();
    private CollectionConfig collection = new CollectionConfig();
    private TimeConfig time = new TimeConfig();

    public List<RssSource> getSources() {
        return sources;
    }

    public void setSources(List<RssSource> sources) {
        this.sources = sources;
    }

    public FilterConfig getFilters() {
        return filters;
    }

    public void setFilters(FilterConfig filters) {
        this.filters = filters;
    }
    
    public CollectionConfig getCollection() {
        return collection;
    }

    public void setCollection(CollectionConfig collection) {
        this.collection = collection;
    }
    
    public TimeConfig getTime() {
        return time;
    }

    public void setTime(TimeConfig time) {
        this.time = time;
    }

    /**
     * 개별 RSS 소스 설정
     */
    public static class RssSource {
        private String name;
        private String code;
        private String url;
        private boolean enabled = true;
        private String category;
        private int updateIntervalMinutes = 30;
        private int priority = 1;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public int getUpdateIntervalMinutes() {
            return updateIntervalMinutes;
        }

        public void setUpdateIntervalMinutes(int updateIntervalMinutes) {
            this.updateIntervalMinutes = updateIntervalMinutes;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    /**
     * 필터링 설정
     */
    public static class FilterConfig {
        private List<String> excludeKeywords = new ArrayList<>();
        private List<String> includeKeywords = new ArrayList<>();
        private int minTitleLength = 10;
        private int maxTitleLength = 200;

        // Getters and Setters
        public List<String> getExcludeKeywords() {
            return excludeKeywords;
        }

        public void setExcludeKeywords(List<String> excludeKeywords) {
            this.excludeKeywords = excludeKeywords;
        }

        public List<String> getIncludeKeywords() {
            return includeKeywords;
        }

        public void setIncludeKeywords(List<String> includeKeywords) {
            this.includeKeywords = includeKeywords;
        }

        public int getMinTitleLength() {
            return minTitleLength;
        }

        public void setMinTitleLength(int minTitleLength) {
            this.minTitleLength = minTitleLength;
        }

        public int getMaxTitleLength() {
            return maxTitleLength;
        }

        public void setMaxTitleLength(int maxTitleLength) {
            this.maxTitleLength = maxTitleLength;
        }
    }

    /**
     * 수집 설정
     */
    public static class CollectionConfig {
        private int maxArticlesPerSource = 50;
        private int connectionTimeoutSec = 10;
        private int readTimeoutSec = 30;
        private int maxRetries = 3;
        private int retryDelayMs = 2000;
        private String userAgent = "EconDigest-RSS-Bot/1.0";
        private DeduplicationConfig deduplication = new DeduplicationConfig();

        // Getters and Setters
        public int getMaxArticlesPerSource() {
            return maxArticlesPerSource;
        }

        public void setMaxArticlesPerSource(int maxArticlesPerSource) {
            this.maxArticlesPerSource = maxArticlesPerSource;
        }

        public int getConnectionTimeoutSec() {
            return connectionTimeoutSec;
        }

        public void setConnectionTimeoutSec(int connectionTimeoutSec) {
            this.connectionTimeoutSec = connectionTimeoutSec;
        }

        public int getReadTimeoutSec() {
            return readTimeoutSec;
        }

        public void setReadTimeoutSec(int readTimeoutSec) {
            this.readTimeoutSec = readTimeoutSec;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryDelayMs() {
            return retryDelayMs;
        }

        public void setRetryDelayMs(int retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public DeduplicationConfig getDeduplication() {
            return deduplication;
        }

        public void setDeduplication(DeduplicationConfig deduplication) {
            this.deduplication = deduplication;
        }
    }

    /**
     * 중복 제거 설정
     */
    public static class DeduplicationConfig {
        private boolean enableUrlDedup = true;
        private boolean enableTitleDedup = true;
        private double titleSimilarityThreshold = 0.85;
        private boolean dedupWithinSourceOnly = false;

        // Getters and Setters
        public boolean isEnableUrlDedup() {
            return enableUrlDedup;
        }

        public void setEnableUrlDedup(boolean enableUrlDedup) {
            this.enableUrlDedup = enableUrlDedup;
        }

        public boolean isEnableTitleDedup() {
            return enableTitleDedup;
        }

        public void setEnableTitleDedup(boolean enableTitleDedup) {
            this.enableTitleDedup = enableTitleDedup;
        }

        public double getTitleSimilarityThreshold() {
            return titleSimilarityThreshold;
        }

        public void setTitleSimilarityThreshold(double titleSimilarityThreshold) {
            this.titleSimilarityThreshold = titleSimilarityThreshold;
        }

        public boolean isDedupWithinSourceOnly() {
            return dedupWithinSourceOnly;
        }

        public void setDedupWithinSourceOnly(boolean dedupWithinSourceOnly) {
            this.dedupWithinSourceOnly = dedupWithinSourceOnly;
        }
    }

    /**
     * 시간 설정
     */
    public static class TimeConfig {
        private String defaultTimeZone = "Asia/Seoul";
        private int maxArticleAgeHours = 72;
        private List<String> dateFormats = new ArrayList<>();

        // Getters and Setters
        public String getDefaultTimeZone() {
            return defaultTimeZone;
        }

        public void setDefaultTimeZone(String defaultTimeZone) {
            this.defaultTimeZone = defaultTimeZone;
        }

        public int getMaxArticleAgeHours() {
            return maxArticleAgeHours;
        }

        public void setMaxArticleAgeHours(int maxArticleAgeHours) {
            this.maxArticleAgeHours = maxArticleAgeHours;
        }

        public List<String> getDateFormats() {
            return dateFormats;
        }

        public void setDateFormats(List<String> dateFormats) {
            this.dateFormats = dateFormats;
        }
    }
}