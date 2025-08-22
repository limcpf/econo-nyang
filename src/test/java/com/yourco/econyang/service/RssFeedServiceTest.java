package com.yourco.econyang.service;

import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * RssFeedService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class RssFeedServiceTest {

    @Mock
    private RssSourcesConfig rssSourcesConfig;

    @InjectMocks
    private RssFeedService rssFeedService;

    private RssSourcesConfig.RssSource testSource;
    private RssSourcesConfig.FilterConfig testFilters;
    private RssSourcesConfig.CollectionConfig collectionConfig;
    private RssSourcesConfig.DeduplicationConfig deduplicationConfig;

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

        // Collection config setup
        collectionConfig = new RssSourcesConfig.CollectionConfig();
        collectionConfig.setMaxRetries(3);
        collectionConfig.setRetryDelayMs(1000);
        collectionConfig.setConnectionTimeoutSec(10);
        collectionConfig.setReadTimeoutSec(30);
        collectionConfig.setUserAgent("EconDigest-Test/1.0");

        // Deduplication config setup
        deduplicationConfig = new RssSourcesConfig.DeduplicationConfig();
        deduplicationConfig.setEnableUrlDedup(true);
        deduplicationConfig.setEnableTitleDedup(true);
        deduplicationConfig.setTitleSimilarityThreshold(0.85);
        collectionConfig.setDeduplication(deduplicationConfig);

        // Mock setup
        when(rssSourcesConfig.getCollection()).thenReturn(collectionConfig);
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

    @Test
    void testApplyFilters_TitleLengthFilter() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        
        // 너무 짧은 제목 (5글자)
        ArticleDto shortTitle = new ArticleDto();
        shortTitle.setTitle("짧은제목경제");
        shortTitle.setDescription("경제 관련");
        shortTitle.setUrl("https://example.com/short");
        articles.add(shortTitle);
        
        // 적절한 제목 (15글자)
        ArticleDto goodTitle = new ArticleDto();
        goodTitle.setTitle("적절한 길이의 경제뉴스 제목입니다");
        goodTitle.setDescription("경제 관련");
        goodTitle.setUrl("https://example.com/good");
        articles.add(goodTitle);
        
        // 너무 긴 제목 (300글자)
        ArticleDto longTitle = new ArticleDto();
        longTitle.setTitle("매우 긴 제목".repeat(50));
        longTitle.setDescription("경제 관련");
        longTitle.setUrl("https://example.com/long");
        articles.add(longTitle);

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, testFilters);

        // Then
        assertEquals(1, result.size());
        assertEquals("적절한 길이의 경제뉴스 제목입니다", result.get(0).getTitle());
    }

    @Test 
    void testApplyFilters_CombinedFilters() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        
        // 모든 조건을 만족하는 기사
        ArticleDto validArticle = new ArticleDto();
        validArticle.setTitle("한국 경제 성장률 발표");
        validArticle.setDescription("올해 경제성장률이 발표되었습니다.");
        validArticle.setUrl("https://example.com/valid");
        articles.add(validArticle);
        
        // 제외 키워드가 있는 기사
        ArticleDto excludedArticle = new ArticleDto();
        excludedArticle.setTitle("경제와 스포츠 투자 전망");
        excludedArticle.setDescription("경제와 스포츠 관련");
        excludedArticle.setUrl("https://example.com/excluded");
        articles.add(excludedArticle);
        
        // 포함 키워드가 없는 기사
        ArticleDto noKeywordArticle = new ArticleDto();
        noKeywordArticle.setTitle("일반 뉴스 제목입니다");
        noKeywordArticle.setDescription("일반적인 뉴스");
        noKeywordArticle.setUrl("https://example.com/nokeyword");
        articles.add(noKeywordArticle);

        // When
        List<ArticleDto> result = rssFeedService.applyFilters(articles, testFilters);

        // Then
        assertEquals(1, result.size());
        assertEquals("한국 경제 성장률 발표", result.get(0).getTitle());
    }
}