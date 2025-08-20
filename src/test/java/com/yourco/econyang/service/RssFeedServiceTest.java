package com.yourco.econyang.service;

import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RssFeedService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class RssFeedServiceTest {

    @InjectMocks
    private RssFeedService rssFeedService;

    private RssSourcesConfig.RssSource testSource;
    private RssSourcesConfig.FilterConfig testFilters;

    @BeforeEach
    void setUp() {
        // Test RSS source setup
        testSource = new RssSourcesConfig.RssSource();
        testSource.setName("테스트 소스");
        testSource.setUrl("https://example.com/rss.xml");
        testSource.setEnabled(true);
        testSource.setCategory("경제");
        testSource.setPriority(1);
        testSource.setUpdateIntervalMinutes(30);

        // Test filter setup
        testFilters = new RssSourcesConfig.FilterConfig();
        testFilters.setIncludeKeywords(Arrays.asList("경제", "금리", "투자"));
        testFilters.setExcludeKeywords(Arrays.asList("스포츠", "연예"));
        testFilters.setMinTitleLength(10);
        testFilters.setMaxTitleLength(200);
    }

    @Test
    void testFetchAllArticles_EmptySourceList() {
        // Given
        List<RssSourcesConfig.RssSource> emptySources = new ArrayList<>();

        // When
        List<ArticleDto> result = rssFeedService.fetchAllArticles(emptySources, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchAllArticles_DisabledSource() {
        // Given
        testSource.setEnabled(false);
        List<RssSourcesConfig.RssSource> sources = Arrays.asList(testSource);

        // When
        List<ArticleDto> result = rssFeedService.fetchAllArticles(sources, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testApplyFilters_NullFilters() {
        // Given
        List<ArticleDto> articles = createTestArticles();

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, null);

        // Then
        assertEquals(articles, result);
    }

    @Test
    void testApplyFilters_IncludeKeywords() {
        // Given
        List<ArticleDto> articles = createTestArticles();

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, testFilters);

        // Then
        assertNotNull(result);
        // 결과는 포함 키워드가 있는 기사들만 포함해야 함
        result.forEach(article -> {
            String content = article.getTitle() + " " + (article.getDescription() != null ? article.getDescription() : "");
            boolean hasIncludeKeyword = testFilters.getIncludeKeywords().stream()
                    .anyMatch(keyword -> content.toLowerCase().contains(keyword.toLowerCase()));
            assertTrue(hasIncludeKeyword, "Article should contain at least one include keyword: " + article.getTitle());
        });
    }

    @Test
    void testApplyFilters_ExcludeKeywords() {
        // Given
        List<ArticleDto> articles = createTestArticles();

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, testFilters);

        // Then
        assertNotNull(result);
        // 결과는 제외 키워드가 없는 기사들만 포함해야 함
        result.forEach(article -> {
            String content = article.getTitle() + " " + (article.getDescription() != null ? article.getDescription() : "");
            boolean hasExcludeKeyword = testFilters.getExcludeKeywords().stream()
                    .anyMatch(keyword -> content.toLowerCase().contains(keyword.toLowerCase()));
            assertFalse(hasExcludeKeyword, "Article should not contain exclude keywords: " + article.getTitle());
        });
    }

    @Test
    void testApplyFilters_EmptyKeywords() {
        // Given
        List<ArticleDto> articles = createTestArticles();
        RssSourcesConfig.FilterConfig emptyFilters = new RssSourcesConfig.FilterConfig();
        emptyFilters.setIncludeKeywords(new ArrayList<>());
        emptyFilters.setExcludeKeywords(new ArrayList<>());

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, emptyFilters);

        // Then
        assertEquals(articles.size(), result.size()); // 필터가 없으면 모든 기사 통과
    }

    @Test
    void testSourceConfiguration() {
        // Given & When & Then
        assertEquals("테스트 소스", testSource.getName());
        assertEquals("https://example.com/rss.xml", testSource.getUrl());
        assertTrue(testSource.isEnabled());
        assertEquals("경제", testSource.getCategory());
        assertEquals(1, testSource.getPriority());
        assertEquals(30, testSource.getUpdateIntervalMinutes());
    }

    @Test
    void testFilterConfiguration() {
        // Given & When & Then
        assertTrue(testFilters.getIncludeKeywords().contains("경제"));
        assertTrue(testFilters.getIncludeKeywords().contains("금리"));
        assertTrue(testFilters.getExcludeKeywords().contains("스포츠"));
        assertTrue(testFilters.getExcludeKeywords().contains("연예"));
        assertEquals(10, testFilters.getMinTitleLength());
        assertEquals(200, testFilters.getMaxTitleLength());
    }

    private List<ArticleDto> createTestArticles() {
        List<ArticleDto> articles = new ArrayList<>();

        // 경제 관련 기사 (포함되어야 함)
        ArticleDto article1 = new ArticleDto();
        article1.setTitle("경제성장률 발표");
        article1.setDescription("올해 경제성장률이 발표되었습니다.");
        article1.setUrl("https://example.com/1");
        articles.add(article1);

        // 금리 관련 기사 (포함되어야 함)
        ArticleDto article2 = new ArticleDto();
        article2.setTitle("기준금리 인상");
        article2.setDescription("중앙은행이 기준금리를 인상했습니다.");
        article2.setUrl("https://example.com/2");
        articles.add(article2);

        // 스포츠 기사 (제외되어야 함)
        ArticleDto article3 = new ArticleDto();
        article3.setTitle("스포츠 경기 결과");
        article3.setDescription("어제 스포츠 경기가 열렸습니다.");
        article3.setUrl("https://example.com/3");
        articles.add(article3);

        // 투자 관련 기사 (포함되어야 함)
        ArticleDto article4 = new ArticleDto();
        article4.setTitle("주식 투자 전망");
        article4.setDescription("올해 주식 투자 전망이 밝습니다.");
        article4.setUrl("https://example.com/4");
        articles.add(article4);

        return articles;
    }
}