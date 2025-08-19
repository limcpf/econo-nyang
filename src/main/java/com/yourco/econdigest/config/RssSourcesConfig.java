package com.yourco.econdigest.config;

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
@ConfigurationProperties(prefix = "")
public class RssSourcesConfig {

    private List<RssSource> sources = new ArrayList<>();
    private GlobalConfig global = new GlobalConfig();
    private FilterConfig filters = new FilterConfig();

    public List<RssSource> getSources() {
        return sources;
    }

    public void setSources(List<RssSource> sources) {
        this.sources = sources;
    }

    public GlobalConfig getGlobal() {
        return global;
    }

    public void setGlobal(GlobalConfig global) {
        this.global = global;
    }

    public FilterConfig getFilters() {
        return filters;
    }

    public void setFilters(FilterConfig filters) {
        this.filters = filters;
    }

    /**
     * 개별 RSS 소스 설정
     */
    public static class RssSource {
        private String name;
        private String code;
        private String url;
        private double weight = 1.0;
        private boolean enabled = true;
        private int timeout = 10000;
        private int retryCount = 3;
        private String description;

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

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 전역 설정
     */
    public static class GlobalConfig {
        private String userAgent = "EconDigest/1.0";
        private int defaultTimeout = 10000;
        private int defaultRetryCount = 3;
        private int maxArticlesPerSource = 20;
        private int minPublishInterval = 3600;

        // Getters and Setters
        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public int getDefaultTimeout() {
            return defaultTimeout;
        }

        public void setDefaultTimeout(int defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        public int getDefaultRetryCount() {
            return defaultRetryCount;
        }

        public void setDefaultRetryCount(int defaultRetryCount) {
            this.defaultRetryCount = defaultRetryCount;
        }

        public int getMaxArticlesPerSource() {
            return maxArticlesPerSource;
        }

        public void setMaxArticlesPerSource(int maxArticlesPerSource) {
            this.maxArticlesPerSource = maxArticlesPerSource;
        }

        public int getMinPublishInterval() {
            return minPublishInterval;
        }

        public void setMinPublishInterval(int minPublishInterval) {
            this.minPublishInterval = minPublishInterval;
        }
    }

    /**
     * 필터링 설정
     */
    public static class FilterConfig {
        private List<String> excludeKeywords = new ArrayList<>();
        private List<String> includeKeywords = new ArrayList<>();

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
    }
}