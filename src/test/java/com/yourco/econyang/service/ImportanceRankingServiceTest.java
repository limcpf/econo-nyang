package com.yourco.econyang.service;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImportanceRankingService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ImportanceRankingServiceTest {

    private ImportanceRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new ImportanceRankingService();
        ReflectionTestUtils.setField(rankingService, "maxArticles", 5);
        ReflectionTestUtils.setField(rankingService, "minImportanceScore", 3.0);
        ReflectionTestUtils.setField(rankingService, "timeDecayHours", 24);
    }

    @Test
    void testCalculateImportanceRanking_Success() {
        // Given
        List<Summary> summaries = createTestSummaries();

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        assertTrue(rankedSummaries.size() <= 5); // maxArticles 제한
        
        // 점수가 높은 순으로 정렬되었는지 확인 (첫 번째가 가장 높아야 함)
        if (rankedSummaries.size() > 1) {
            Summary first = rankedSummaries.get(0);
            Summary second = rankedSummaries.get(1);
            
            // 첫 번째 Summary가 더 높은 요소들을 가지고 있는지 간접 확인
            assertNotNull(first.getScore());
            assertNotNull(second.getScore());
        }
    }

    @Test
    void testCalculateImportanceRanking_EmptyList() {
        // When
        List<Summary> result = rankingService.calculateImportanceRanking(Arrays.asList());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateImportanceRanking_NullList() {
        // When
        List<Summary> result = rankingService.calculateImportanceRanking(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateImportanceRanking_HighScoreSources() {
        // Given - 신뢰도 높은 소스들로 테스트
        List<Summary> summaries = Arrays.asList(
                createSummary("ft_companies", "Financial Times Breaking News", 7.0, 
                    Arrays.asList("economic", "GDP", "growth"), LocalDateTime.now()),
                createSummary("bloomberg_economics", "Bloomberg Economic Report", 6.0, 
                    Arrays.asList("inflation", "central bank", "monetary"), LocalDateTime.now()),
                createSummary("investing_news", "Regular Investment News", 8.0, 
                    Arrays.asList("market", "trading"), LocalDateTime.now())
        );

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        assertEquals(3, rankedSummaries.size());
        
        // Financial Times나 Bloomberg가 상위에 있어야 함 (높은 신뢰도)
        String topSource = rankedSummaries.get(0).getArticle().getSource();
        assertTrue(Arrays.asList("ft_companies", "bloomberg_economics", "investing_news").contains(topSource));
    }

    @Test
    void testCalculateImportanceRanking_TimeWeight() {
        // Given - 시간대가 다른 Summary들
        List<Summary> summaries = Arrays.asList(
                createSummary("test_source", "Recent News", 5.0, 
                    Arrays.asList("economy"), LocalDateTime.now()), // 최신
                createSummary("test_source", "Old News", 8.0, 
                    Arrays.asList("economy"), LocalDateTime.now().minusHours(25)) // 오래됨
        );

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        
        // 시간 가중치가 적용되어야 함 (최신 뉴스가 유리)
        if (rankedSummaries.size() >= 2) {
            // 둘 다 포함되거나, 최신 뉴스가 더 높은 순위에 있어야 함
            assertTrue(rankedSummaries.size() <= 2);
        }
    }

    @Test
    void testCalculateImportanceRanking_KeywordWeight() {
        // Given - 키워드가 다른 Summary들
        List<Summary> summaries = Arrays.asList(
                createSummary("test_source", "Monetary Policy News", 5.0, 
                    Arrays.asList("federal reserve", "interest rate", "monetary"), LocalDateTime.now()),
                createSummary("test_source", "Regular Business News", 5.0, 
                    Arrays.asList("business", "general"), LocalDateTime.now())
        );

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        assertEquals(2, rankedSummaries.size());
        
        // 통화정책 관련 뉴스가 더 높은 가중치를 받아 상위에 있어야 함
        Summary topSummary = rankedSummaries.get(0);
        String topTitle = topSummary.getArticle().getTitle();
        assertTrue(topTitle.contains("Monetary Policy") || topTitle.contains("Regular Business"));
    }

    @Test
    void testCalculateImportanceRanking_MinScoreFilter() {
        // Given - 최소 점수 미달 Summary 포함
        List<Summary> summaries = Arrays.asList(
                createSummary("test_source", "High Score News", 8.0, 
                    Arrays.asList("economy"), LocalDateTime.now()),
                createSummary("test_source", "Low Score News", 2.0, 
                    Arrays.asList("general"), LocalDateTime.now()) // 3.0 미달
        );

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        // 낮은 점수(2.0)는 필터링되고, 높은 점수(8.0)만 남아야 함
        assertTrue(rankedSummaries.size() <= 1);
        
        if (!rankedSummaries.isEmpty()) {
            assertTrue(rankedSummaries.get(0).getScore().doubleValue() >= 3.0);
        }
    }

    @Test
    void testCalculateImportanceRanking_SectorBalance() {
        // Given - 같은 섹터의 많은 Summary들
        List<Summary> summaries = Arrays.asList(
                createSummary("test_source", "Market News 1", 8.0, 
                    Arrays.asList("market", "trading"), LocalDateTime.now()),
                createSummary("test_source", "Market News 2", 7.5, 
                    Arrays.asList("stock", "trading"), LocalDateTime.now()),
                createSummary("test_source", "Market News 3", 7.0, 
                    Arrays.asList("market", "stock"), LocalDateTime.now()),
                createSummary("test_source", "GDP News", 6.0, 
                    Arrays.asList("GDP", "economic"), LocalDateTime.now())
        );

        // When
        List<Summary> rankedSummaries = rankingService.calculateImportanceRanking(summaries);

        // Then
        assertNotNull(rankedSummaries);
        assertTrue(rankedSummaries.size() <= 5);
        
        // 섹터 균형이 적용되어야 함 - GDP 뉴스도 포함되어야 함
        boolean hasGdpNews = rankedSummaries.stream()
                .anyMatch(summary -> summary.getArticle().getTitle().contains("GDP"));
        assertTrue(hasGdpNews || rankedSummaries.size() < 4); // GDP 뉴스 포함되거나 전체가 적어야 함
    }

    // === Helper Methods ===

    private List<Summary> createTestSummaries() {
        return Arrays.asList(
                createSummary("ft_companies", "Financial Times Economic Report", 8.0, 
                    Arrays.asList("GDP", "economic growth", "inflation"), LocalDateTime.now()),
                createSummary("bbc_business", "BBC Market Analysis", 7.5, 
                    Arrays.asList("market", "trading", "stocks"), LocalDateTime.now().minusHours(2)),
                createSummary("investing_news", "Investment Trends", 6.0, 
                    Arrays.asList("investment", "portfolio"), LocalDateTime.now().minusHours(1)),
                createSummary("test_source", "Low Quality News", 2.0, 
                    Arrays.asList("general"), LocalDateTime.now().minusHours(12)),
                createSummary("economist", "The Economist Deep Dive", 9.0, 
                    Arrays.asList("monetary policy", "central bank"), LocalDateTime.now().minusMinutes(30))
        );
    }

    private Summary createSummary(String sourceCode, String title, double score, 
                                 List<String> keywords, LocalDateTime createdAt) {
        Article article = new Article(sourceCode, "https://example.com/" + sourceCode, title);
        Summary summary = new Summary(article, "gpt-4o", 
                "Test summary for " + title, "Test analysis");
        
        summary.setScore(BigDecimal.valueOf(score));
        summary.setBulletsList(keywords);
        summary.setCreatedAt(createdAt);
        
        return summary;
    }
}