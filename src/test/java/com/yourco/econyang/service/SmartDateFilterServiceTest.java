package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.strategy.SmartDateFilterStrategy;
import com.yourco.econyang.strategy.SmartDateFilterStrategy.DateEstimationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SmartDateFilterServiceTest {

    private SmartDateFilterService smartDateFilterService;

    @Mock
    private SmartDateFilterStrategy mockStrategy;

    @Mock
    private ArticleDateCacheService mockCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        smartDateFilterService = new SmartDateFilterService();
        
        // Mock 객체들을 서비스에 주입 (reflection 사용)
        try {
            java.lang.reflect.Field strategiesField = SmartDateFilterService.class.getDeclaredField("smartStrategies");
            strategiesField.setAccessible(true);
            strategiesField.set(smartDateFilterService, Arrays.asList(mockStrategy));
            
            java.lang.reflect.Field cacheField = SmartDateFilterService.class.getDeclaredField("cacheService");
            cacheField.setAccessible(true);
            cacheField.set(smartDateFilterService, mockCacheService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_filter_articles_with_valid_dates() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusHours(24);
        
        ArticleDto recentArticle = createArticle("Recent News", "Financial Times", "http://ft.com/recent");
        ArticleDto oldArticle = createArticle("Old News", "Financial Times", "http://ft.com/old");
        
        List<ArticleDto> articles = Arrays.asList(recentArticle, oldArticle);
        
        // Mock strategy setup
        when(mockStrategy.supports(anyString())).thenReturn(true);
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        
        // Mock successful estimation for recent article
        DateEstimationResult recentResult = new DateEstimationResult(
                Optional.of(now.minusHours(12)), 0.8, "url", "Mock extraction");
        when(mockStrategy.estimateDateWithFallbackChain(eq(recentArticle), anyInt(), anyInt(), anyString()))
                .thenReturn(recentResult);
        
        // Mock failed estimation for old article
        DateEstimationResult oldResult = new DateEstimationResult(
                Optional.of(now.minusHours(36)), 0.2, "failed", "Too old");
        when(mockStrategy.estimateDateWithFallbackChain(eq(oldArticle), anyInt(), anyInt(), anyString()))
                .thenReturn(oldResult);
        
        // When
        List<ArticleDto> result = smartDateFilterService.filterArticlesWithSmartStrategies(articles, cutoffTime);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("Recent News", result.get(0).getTitle());
        assertNotNull(result.get(0).getPublishedAt());
        assertTrue(result.get(0).getPublishedAt().isAfter(cutoffTime));
    }

    @Test
    void should_group_articles_by_source() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        ArticleDto ftArticle = createArticle("FT News", "Financial Times", "http://ft.com/news");
        ArticleDto bbcArticle = createArticle("BBC News", "BBC Business", "http://bbc.com/news");
        
        List<ArticleDto> articles = Arrays.asList(ftArticle, bbcArticle);
        
        when(mockStrategy.supports(anyString())).thenReturn(true);
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        
        // Mock successful estimations
        DateEstimationResult successResult = new DateEstimationResult(
                Optional.of(LocalDateTime.now().minusHours(12)), 0.8, "url", "Success");
        when(mockStrategy.estimateDateWithFallbackChain(any(), anyInt(), anyInt(), anyString()))
                .thenReturn(successResult);
        
        // When
        List<ArticleDto> result = smartDateFilterService.filterArticlesWithSmartStrategies(articles, cutoffTime);
        
        // Then
        assertEquals(2, result.size());
        
        // Verify that strategy was called for each source
        verify(mockStrategy, times(2)).estimateDateWithFallbackChain(any(), anyInt(), anyInt(), anyString());
    }

    @Test
    void should_handle_articles_without_matching_strategy() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        ArticleDto unknownArticle = createArticle("Unknown News", "Unknown Source", "http://unknown.com/news");
        List<ArticleDto> articles = Arrays.asList(unknownArticle);
        
        // Mock strategy that doesn't support this source
        when(mockStrategy.supports(anyString())).thenReturn(false);
        
        // When
        List<ArticleDto> result = smartDateFilterService.filterArticlesWithSmartStrategies(articles, cutoffTime);
        
        // Then
        assertEquals(1, result.size()); // Should use basic strategy
        assertNotNull(result.get(0).getPublishedAt());
    }

    @Test
    void should_handle_empty_article_list() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ArticleDto> articles = Arrays.asList();
        
        // When
        List<ArticleDto> result = smartDateFilterService.filterArticlesWithSmartStrategies(articles, cutoffTime);
        
        // Then
        assertEquals(0, result.size());
    }

    @Test
    void should_handle_invalid_estimation_results() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        ArticleDto article = createArticle("Test News", "Financial Times", "http://ft.com/test");
        List<ArticleDto> articles = Arrays.asList(article);
        
        when(mockStrategy.supports(anyString())).thenReturn(true);
        when(mockStrategy.getStrategyName()).thenReturn("MockStrategy");
        
        // Mock invalid estimation result
        DateEstimationResult invalidResult = new DateEstimationResult(
                Optional.empty(), 0.0, "failed", "Estimation failed");
        when(mockStrategy.estimateDateWithFallbackChain(any(), anyInt(), anyInt(), anyString()))
                .thenReturn(invalidResult);
        
        // When
        List<ArticleDto> result = smartDateFilterService.filterArticlesWithSmartStrategies(articles, cutoffTime);
        
        // Then
        assertEquals(0, result.size()); // Invalid results should be filtered out
    }

    private ArticleDto createArticle(String title, String source, String url) {
        ArticleDto article = new ArticleDto();
        article.setTitle(title);
        article.setSource(source);
        article.setUrl(url);
        return article;
    }
}