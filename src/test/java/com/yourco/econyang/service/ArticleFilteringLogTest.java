package com.yourco.econyang.service;

import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 기사 필터링 로깅 기능 테스트
 */
public class ArticleFilteringLogTest {

    private RssFeedService rssFeedService;

    @Mock
    private RssSourcesConfig mockRssConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rssFeedService = new RssFeedService(mockRssConfig);
    }

    @Test
    void should_log_title_length_filtering() {
        // Given
        List<ArticleDto> articles = Arrays.asList(
                createArticle("This title is good", "Financial Times"), // 18자 - 통과
                createArticle("This is a very long title that exceeds maximum length limit for testing purposes", "Financial Times") // 80자 - 제외
        );
        
        RssSourcesConfig.FilterConfig filters = createFilterConfig(10, 50, null, null);

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, filters);

        // Then
        assertEquals(1, result.size()); // Only one should pass length filter
        assertEquals("This title is good", result.get(0).getTitle());
    }

    @Test
    void should_log_include_keyword_filtering() {
        // Given
        List<ArticleDto> articles = Arrays.asList(
                createArticle("Stock market rises", "Financial Times"),
                createArticle("Weather report today", "Financial Times")
        );
        
        RssSourcesConfig.FilterConfig filters = createFilterConfig(5, 100, Arrays.asList("stock", "market"), null);

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, filters);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTitle().toLowerCase().contains("stock"));
    }

    @Test
    void should_log_exclude_keyword_filtering() {
        // Given
        List<ArticleDto> articles = Arrays.asList(
                createArticle("Stock market analysis", "Financial Times"),
                createArticle("Sports news today", "Financial Times")
        );
        
        RssSourcesConfig.FilterConfig filters = createFilterConfig(5, 100, null, Arrays.asList("sports"));

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, filters);

        // Then
        assertEquals(1, result.size());
        assertFalse(result.get(0).getTitle().toLowerCase().contains("sports"));
    }

    @Test
    void should_pass_through_when_no_filters() {
        // Given
        List<ArticleDto> articles = Arrays.asList(
                createArticle("Article 1", "Financial Times"),
                createArticle("Article 2", "Financial Times")
        );

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, null);

        // Then
        assertEquals(2, result.size());
    }

    private ArticleDto createArticle(String title, String source) {
        ArticleDto article = new ArticleDto();
        article.setTitle(title);
        article.setSource(source);
        article.setUrl("http://example.com/" + title.replaceAll("\\s+", ""));
        article.setDescription("Description for " + title);
        article.setPublishedAt(LocalDateTime.now());
        return article;
    }

    private RssSourcesConfig.FilterConfig createFilterConfig(int minLength, int maxLength, 
                                                            List<String> includeKeywords, List<String> excludeKeywords) {
        RssSourcesConfig.FilterConfig filter = new RssSourcesConfig.FilterConfig();
        filter.setMinTitleLength(minLength);
        filter.setMaxTitleLength(maxLength);
        filter.setIncludeKeywords(includeKeywords);
        filter.setExcludeKeywords(excludeKeywords);
        return filter;
    }
}