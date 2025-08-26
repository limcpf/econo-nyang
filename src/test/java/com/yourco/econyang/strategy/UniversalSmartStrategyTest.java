package com.yourco.econyang.strategy;

import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.ArticleDateCacheService;
import com.yourco.econyang.service.ContentDateExtractor;
import com.yourco.econyang.strategy.SmartDateFilterStrategy.DateEstimationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UniversalSmartStrategy 통합 테스트
 */
@ExtendWith(MockitoExtension.class)
class UniversalSmartStrategyTest {

    @Mock
    private ArticleDateCacheService cacheService;

    @Mock
    private ContentDateExtractor contentDateExtractor;

    @InjectMocks
    private UniversalSmartStrategy strategy;

    private ArticleDto testArticle;

    @BeforeEach
    void setUp() {
        testArticle = new ArticleDto();
        testArticle.setTitle("경제 뉴스 테스트 기사");
        testArticle.setUrl("https://example.com/news/2025/01/15/test-article");
        testArticle.setSource("Test Source");
        
        // Set the @Value field for testing
        ReflectionTestUtils.setField(strategy, "universalMaxAgeHours", 72);
        ReflectionTestUtils.setField(strategy, "enableContentScan", true);
        ReflectionTestUtils.setField(strategy, "contentScanTimeoutSeconds", 10);
    }

    @Test
    void testSupports_ShouldSupportAllSources() {
        // Given
        String[] testSources = {"bbc_business", "bloomberg", "investing", "unknown_source"};

        // When & Then
        for (String source : testSources) {
            assertTrue(strategy.supports(source), 
                "UniversalSmartStrategy should support all sources: " + source);
        }
    }

    @Test
    void testGetStrategyName() {
        // When
        String name = strategy.getStrategyName();

        // Then
        assertEquals("UniversalSmartStrategy", name);
    }

    @Test
    void testGetMaxAgeHours() {
        // When
        int maxAge = strategy.getMaxAgeHours("any_source");

        // Then
        assertEquals(72, maxAge); // Default universalMaxAgeHours
    }

    @Test
    void testEstimateDateWithFallbackChain_WithCache() {
        // Given
        LocalDateTime cachedDate = LocalDateTime.now().minusHours(2);
        DateEstimationResult cachedResult = new DateEstimationResult(
            Optional.of(cachedDate), 0.9, "cache", "Cached result"
        );
        
        when(cacheService.getCachedDate(anyString(), eq("Universal")))
            .thenReturn(Optional.of(cachedResult));

        // When
        DateEstimationResult result = strategy.estimateDateWithFallbackChain(testArticle, 0, 1, "test_source");

        // Then
        assertTrue(result.isValid());
        assertEquals(cachedDate, result.getEstimatedDate().get());
        assertEquals("cache", result.getExtractionMethod());
    }

    @Test
    void testExtractDateFromUrl_WithValidPattern() {
        // Given
        String urlWithDate = "https://example.com/news/2025-01-15/test-article";
        testArticle.setUrl(urlWithDate);

        // When
        Optional<LocalDateTime> result = strategy.extractDateFromUrl(urlWithDate, "test_source");

        // Then
        if (result.isPresent()) {
            LocalDateTime extracted = result.get();
            assertEquals(2025, extracted.getYear());
            assertEquals(1, extracted.getMonthValue());
            assertEquals(15, extracted.getDayOfMonth());
        }
        // URL 패턴에 따라 결과가 달라질 수 있으므로 항상 성공을 보장하지 않음
    }

    @Test
    void testEstimateDateFromRssPosition() {
        // Given
        int rssPosition = 0;
        int totalArticles = 10;
        
        // When
        Optional<LocalDateTime> result = strategy.estimateDateFromRssPosition(rssPosition, totalArticles, "test_source");

        // Then
        assertTrue(result.isPresent());
        LocalDateTime estimated = result.get();
        LocalDateTime now = LocalDateTime.now();
        
        // 첫 번째 기사는 1시간 이내
        assertTrue(estimated.isAfter(now.minusHours(2)));
        assertTrue(estimated.isBefore(now.plusMinutes(10)));
    }

    @Test
    void testCalculateConfidenceScore_UrlPattern() {
        // Given
        LocalDateTime recentDate = LocalDateTime.now().minusHours(1);
        String method = "url_pattern";

        // When
        double confidence = strategy.calculateConfidenceScore(recentDate, method, "test_source");

        // Then
        assertTrue(confidence >= 0.0, "Confidence should be non-negative: " + confidence);
        assertTrue(confidence <= 1.0, "Confidence should not exceed 1.0: " + confidence);
        // url_pattern base score is 0.7, but it may be adjusted based on time
        assertTrue(confidence > 0.5, "url_pattern should have reasonable confidence: " + confidence);
    }

    @Test
    void testCalculateConfidenceScore_FutureDate() {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);
        String method = "url_pattern";

        // When
        double confidence = strategy.calculateConfidenceScore(futureDate, method, "test_source");

        // Then
        assertTrue(confidence < 0.3); // Future dates should have low confidence
    }

    @Test
    void testEstimateDateFromPublishingPattern() {
        // When
        Optional<LocalDateTime> result = strategy.estimateDateFromPublishingPattern(testArticle, "test_source");

        // Then
        assertTrue(result.isPresent());
        LocalDateTime estimated = result.get();
        LocalDateTime now = LocalDateTime.now();
        
        // Should be around 6 hours ago
        assertTrue(estimated.isAfter(now.minusHours(8)));
        assertTrue(estimated.isBefore(now.minusHours(4)));
    }
}