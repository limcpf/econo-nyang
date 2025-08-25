package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BinarySearchDateFilterServiceTest {

    private BinarySearchDateFilterService binarySearchService;
    private ContentDateExtractorService mockDateExtractor;

    @BeforeEach
    void setUp() {
        mockDateExtractor = mock(ContentDateExtractorService.class);
        binarySearchService = new BinarySearchDateFilterService(mockDateExtractor);
    }

    @Test
    void should_filter_articles_within_24_hours() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusHours(24);
        
        ArticleDto recentArticle = createArticle("Recent News", "Financial Times", "http://example.com/recent");
        ArticleDto oldArticle = createArticle("Old News", "Financial Times", "http://example.com/old");
        
        List<ArticleDto> articles = Arrays.asList(recentArticle, oldArticle);
        
        // Mock 날짜 추출 결과
        when(mockDateExtractor.extractPublishedDate("http://example.com/recent", "Financial Times"))
                .thenReturn(Optional.of(now.minusHours(12))); // 12시간 전 (유효)
        when(mockDateExtractor.extractPublishedDate("http://example.com/old", "Financial Times"))
                .thenReturn(Optional.of(now.minusHours(36))); // 36시간 전 (무효)
        
        // When
        List<ArticleDto> result = binarySearchService.filterArticlesWithBinarySearch(articles, cutoffTime);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("Recent News", result.get(0).getTitle());
    }

    @Test
    void should_group_articles_by_source() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        ArticleDto ftArticle = createArticle("FT News", "Financial Times", "http://ft.com/news");
        ArticleDto bbcArticle = createArticle("BBC News", "BBC", "http://bbc.com/news");
        
        List<ArticleDto> articles = Arrays.asList(ftArticle, bbcArticle);
        
        // Mock 모든 기사가 유효하도록 설정
        when(mockDateExtractor.extractPublishedDate(anyString(), anyString()))
                .thenReturn(Optional.of(LocalDateTime.now().minusHours(12)));
        
        // When
        List<ArticleDto> result = binarySearchService.filterArticlesWithBinarySearch(articles, cutoffTime);
        
        // Then
        assertEquals(2, result.size());
        
        // 각 언론사별로 한 번씩 날짜 추출이 호출되어야 함
        verify(mockDateExtractor).extractPublishedDate("http://ft.com/news", "Financial Times");
        verify(mockDateExtractor).extractPublishedDate("http://bbc.com/news", "BBC");
    }

    @Test
    void should_exclude_articles_without_extractable_date() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        ArticleDto article = createArticle("No Date Article", "Financial Times", "http://example.com/nodate");
        List<ArticleDto> articles = Arrays.asList(article);
        
        // Mock 날짜 추출 실패
        when(mockDateExtractor.extractPublishedDate("http://example.com/nodate", "Financial Times"))
                .thenReturn(Optional.empty());
        
        // When
        List<ArticleDto> result = binarySearchService.filterArticlesWithBinarySearch(articles, cutoffTime);
        
        // Then
        assertEquals(0, result.size());
    }

    @Test
    void should_handle_empty_article_list() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ArticleDto> articles = Arrays.asList();
        
        // When
        List<ArticleDto> result = binarySearchService.filterArticlesWithBinarySearch(articles, cutoffTime);
        
        // Then
        assertEquals(0, result.size());
    }

    @Test
    void should_handle_articles_from_multiple_sources() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusHours(24);
        
        ArticleDto ftRecent = createArticle("FT Recent", "Financial Times", "http://ft.com/recent");
        ArticleDto ftOld = createArticle("FT Old", "Financial Times", "http://ft.com/old");
        ArticleDto bbcRecent = createArticle("BBC Recent", "BBC", "http://bbc.com/recent");
        
        List<ArticleDto> articles = Arrays.asList(ftRecent, ftOld, bbcRecent);
        
        // Mock 설정
        when(mockDateExtractor.extractPublishedDate("http://ft.com/recent", "Financial Times"))
                .thenReturn(Optional.of(now.minusHours(12)));
        when(mockDateExtractor.extractPublishedDate("http://ft.com/old", "Financial Times"))
                .thenReturn(Optional.of(now.minusHours(36)));
        when(mockDateExtractor.extractPublishedDate("http://bbc.com/recent", "BBC"))
                .thenReturn(Optional.of(now.minusHours(6)));
        
        // When
        List<ArticleDto> result = binarySearchService.filterArticlesWithBinarySearch(articles, cutoffTime);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("FT Recent")));
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("BBC Recent")));
    }

    private ArticleDto createArticle(String title, String source, String url) {
        ArticleDto article = new ArticleDto();
        article.setTitle(title);
        article.setSource(source);
        article.setUrl(url);
        return article;
    }
}