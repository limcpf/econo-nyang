package com.yourco.econyang.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RssSourcesConfig 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class RssSourcesConfigTest {

    @Autowired
    private RssSourcesConfig rssSourcesConfig;

    @Test
    void testRssSourcesLoaded() {
        List<RssSourcesConfig.RssSource> sources = rssSourcesConfig.getSources();
        
        assertNotNull(sources);
        assertTrue(sources.size() > 0);
        
        RssSourcesConfig.RssSource firstSource = sources.get(0);
        assertNotNull(firstSource.getName());
        assertNotNull(firstSource.getUrl());
        assertNotNull(firstSource.getCategory());
        assertTrue(firstSource.getPriority() >= 1);
        assertTrue(firstSource.getUpdateIntervalMinutes() > 0);
    }

    @Test
    void testFiltersConfiguration() {
        RssSourcesConfig.FilterConfig filters = rssSourcesConfig.getFilters();
        
        assertNotNull(filters);
        assertNotNull(filters.getIncludeKeywords());
        assertNotNull(filters.getExcludeKeywords());
        assertTrue(filters.getIncludeKeywords().size() > 0);
        assertTrue(filters.getMinTitleLength() > 0);
        assertTrue(filters.getMaxTitleLength() > filters.getMinTitleLength());
    }

    @Test
    void testCollectionConfiguration() {
        RssSourcesConfig.CollectionConfig collection = rssSourcesConfig.getCollection();
        
        assertNotNull(collection);
        assertTrue(collection.getMaxArticlesPerSource() > 0);
        assertTrue(collection.getConnectionTimeoutSec() > 0);
        assertTrue(collection.getReadTimeoutSec() > 0);
        assertTrue(collection.getMaxRetries() >= 1);
        assertNotNull(collection.getUserAgent());
        
        RssSourcesConfig.DeduplicationConfig deduplication = collection.getDeduplication();
        assertNotNull(deduplication);
        assertTrue(deduplication.getTitleSimilarityThreshold() > 0.0);
        assertTrue(deduplication.getTitleSimilarityThreshold() <= 1.0);
    }

    @Test
    void testTimeConfiguration() {
        RssSourcesConfig.TimeConfig time = rssSourcesConfig.getTime();
        
        assertNotNull(time);
        assertNotNull(time.getDefaultTimeZone());
        assertTrue(time.getMaxArticleAgeHours() > 0);
        assertNotNull(time.getDateFormats());
    }

    @Test
    void testEconomicKeywords() {
        List<String> includeKeywords = rssSourcesConfig.getFilters().getIncludeKeywords();
        
        // 핵심 경제 키워드들이 포함되어 있는지 확인
        assertTrue(includeKeywords.contains("경제"));
        assertTrue(includeKeywords.contains("금리"));
        assertTrue(includeKeywords.contains("투자"));
        assertTrue(includeKeywords.contains("주식"));
        assertTrue(includeKeywords.contains("부동산"));
    }

    @Test
    void testExcludeKeywords() {
        List<String> excludeKeywords = rssSourcesConfig.getFilters().getExcludeKeywords();
        
        // 제외할 키워드들이 포함되어 있는지 확인
        assertTrue(excludeKeywords.contains("스포츠"));
        assertTrue(excludeKeywords.contains("연예"));
    }
}