package com.yourco.econdigest.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSS 소스 설정 로드 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class RssSourcesConfigTest {

    @Autowired
    private RssSourcesConfig rssSourcesConfig;

    @Test
    void testRssSourcesConfigLoaded() {
        // RSS 소스 설정이 정상적으로 로드되었는지 확인
        assertNotNull(rssSourcesConfig);
        assertNotNull(rssSourcesConfig.getSources());
        assertFalse(rssSourcesConfig.getSources().isEmpty());
        
        System.out.println("로드된 RSS 소스 수: " + rssSourcesConfig.getSources().size());
        
        // 각 소스의 기본 정보 확인
        for (RssSourcesConfig.RssSource source : rssSourcesConfig.getSources()) {
            assertNotNull(source.getName());
            assertNotNull(source.getCode());
            assertNotNull(source.getUrl());
            assertTrue(source.getWeight() > 0);
            assertTrue(source.getTimeout() > 0);
            assertTrue(source.getRetryCount() >= 0);
            
            System.out.println("RSS 소스: " + source.getName() + " (" + source.getCode() + ") - " + 
                             "가중치: " + source.getWeight() + ", 활성화: " + source.isEnabled());
        }
    }

    @Test
    void testGlobalConfig() {
        // 전역 설정 확인
        RssSourcesConfig.GlobalConfig global = rssSourcesConfig.getGlobal();
        assertNotNull(global);
        assertNotNull(global.getUserAgent());
        assertTrue(global.getDefaultTimeout() > 0);
        assertTrue(global.getDefaultRetryCount() >= 0);
        assertTrue(global.getMaxArticlesPerSource() > 0);
        assertTrue(global.getMinPublishInterval() >= 0);
        
        System.out.println("전역 설정:");
        System.out.println("  User-Agent: " + global.getUserAgent());
        System.out.println("  기본 타임아웃: " + global.getDefaultTimeout() + "ms");
        System.out.println("  기본 재시도: " + global.getDefaultRetryCount() + "회");
        System.out.println("  소스당 최대 기사: " + global.getMaxArticlesPerSource() + "개");
    }

    @Test
    void testFilterConfig() {
        // 필터 설정 확인
        RssSourcesConfig.FilterConfig filters = rssSourcesConfig.getFilters();
        assertNotNull(filters);
        assertNotNull(filters.getExcludeKeywords());
        assertNotNull(filters.getIncludeKeywords());
        
        System.out.println("필터 설정:");
        System.out.println("  제외 키워드 수: " + filters.getExcludeKeywords().size());
        System.out.println("  포함 키워드 수: " + filters.getIncludeKeywords().size());
        
        if (!filters.getExcludeKeywords().isEmpty()) {
            System.out.println("  제외 키워드: " + filters.getExcludeKeywords());
        }
        
        if (!filters.getIncludeKeywords().isEmpty()) {
            System.out.println("  포함 키워드: " + filters.getIncludeKeywords());
        }
    }

    @Test
    void testSpecificSources() {
        // 특정 소스들이 올바르게 설정되었는지 확인
        boolean hasHankyung = rssSourcesConfig.getSources().stream()
                .anyMatch(source -> "hankyung".equals(source.getCode()));
        boolean hasGoogleNews = rssSourcesConfig.getSources().stream()
                .anyMatch(source -> "google_kr_econ".equals(source.getCode()));
        
        assertTrue(hasHankyung, "한국경제 소스가 설정되어야 함");
        assertTrue(hasGoogleNews, "Google 뉴스 소스가 설정되어야 함");
        
        // 각 소스의 URL이 유효한 형태인지 확인
        for (RssSourcesConfig.RssSource source : rssSourcesConfig.getSources()) {
            assertTrue(source.getUrl().startsWith("http"), 
                     "RSS URL이 http로 시작해야 함: " + source.getUrl());
        }
    }

    @Test
    void testEnabledSources() {
        // 활성화된 소스가 최소 1개는 있어야 함
        long enabledCount = rssSourcesConfig.getSources().stream()
                .mapToLong(source -> source.isEnabled() ? 1 : 0)
                .sum();
        
        assertTrue(enabledCount > 0, "활성화된 RSS 소스가 최소 1개는 있어야 함");
        
        System.out.println("활성화된 소스 수: " + enabledCount + "/" + rssSourcesConfig.getSources().size());
    }

    @Test
    void testKeywordFilters() {
        // 키워드 필터가 적절히 설정되었는지 확인
        RssSourcesConfig.FilterConfig filters = rssSourcesConfig.getFilters();
        
        // 경제 관련 키워드가 포함 키워드에 있는지 확인
        boolean hasEconomicKeywords = filters.getIncludeKeywords().stream()
                .anyMatch(keyword -> keyword.contains("경제") || keyword.contains("금리") || keyword.contains("투자"));
        
        assertTrue(hasEconomicKeywords, "경제 관련 포함 키워드가 설정되어야 함");
        
        // 부적절한 키워드가 제외 키워드에 있는지 확인
        boolean hasExcludeKeywords = filters.getExcludeKeywords().stream()
                .anyMatch(keyword -> keyword.contains("스포츠") || keyword.contains("연예"));
        
        assertTrue(hasExcludeKeywords, "부적절한 제외 키워드가 설정되어야 함");
    }
}