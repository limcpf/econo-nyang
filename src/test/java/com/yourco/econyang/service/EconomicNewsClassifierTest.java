package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.EconomicNewsClassifier.NewsQualityScore;
import com.yourco.econyang.service.EconomicNewsClassifier.NewsCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 경제 뉴스 분류기 테스트
 */
public class EconomicNewsClassifierTest {

    private EconomicNewsClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new EconomicNewsClassifier();
    }

    @Test
    void should_give_high_score_for_stock_market_news() {
        // Given
        ArticleDto article = createArticle(
            "Apple stock soars on strong quarterly earnings report", 
            "Apple reported record revenue and earnings per share, driving stock price up 15% in after-hours trading"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertTrue(score.getScore() >= 6, "Stock market news should get high score");
        assertEquals(NewsCategory.HIGH_PRIORITY, score.getCategory());
        assertTrue(score.getReason().contains("핵심경제"));
    }

    @Test
    void should_give_high_score_for_macroeconomic_news() {
        // Given
        ArticleDto article = createArticle(
            "Federal Reserve raises interest rates to combat inflation",
            "The Fed announced a 0.5% rate hike as inflation reaches 8.5% annually, highest in 40 years"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertTrue(score.getScore() >= 6, "Macro economic news should get high score");
        assertEquals(NewsCategory.HIGH_PRIORITY, score.getCategory());
    }

    @Test
    void should_allow_real_estate_and_commodities_news() {
        // Given
        ArticleDto article = createArticle(
            "Gold prices surge amid real estate market volatility",
            "Gold reached $2000/oz while REIT investments show mixed results in commercial real estate sector"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertTrue(score.getScore() >= 2, "Investment asset news should be allowed");
        assertTrue(score.getCategory() == NewsCategory.ALLOWED_INVESTMENT || 
                  score.getCategory() == NewsCategory.MEDIUM_PRIORITY ||
                  score.getCategory() == NewsCategory.HIGH_PRIORITY);
        assertTrue(score.getReason().contains("투자자산"));
    }

    @Test
    void should_exclude_entertainment_news() {
        // Given
        ArticleDto article = createArticle(
            "Celebrity movie premiere draws huge crowds",
            "Hollywood stars attended the entertainment industry's biggest event of the year"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertEquals(0, score.getScore());
        assertEquals(NewsCategory.EXCLUDED, score.getCategory());
        assertTrue(score.getReason().contains("강력 제외 키워드"));
    }

    @Test
    void should_exclude_sports_news() {
        // Given
        ArticleDto article = createArticle(
            "Football team wins championship game",
            "The sports event attracted millions of viewers worldwide"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertEquals(0, score.getScore());
        assertEquals(NewsCategory.EXCLUDED, score.getCategory());
    }

    @Test
    void should_exclude_health_news() {
        // Given
        ArticleDto article = createArticle(
            "New medical treatment shows promise for disease",
            "Hospital doctors report breakthrough in vaccine development"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertEquals(0, score.getScore());
        assertEquals(NewsCategory.EXCLUDED, score.getCategory());
    }

    @Test
    void should_handle_korean_economic_news() {
        // Given
        ArticleDto article = createArticle(
            "코스피 상승세 지속, 삼성전자 실적 호조",
            "한국은행 기준금리 인상 결정으로 주식시장이 상승 반응을 보이고 있다"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertTrue(score.getScore() >= 6, "Korean economic news should get high score");
        assertEquals(NewsCategory.HIGH_PRIORITY, score.getCategory());
    }

    @Test
    void should_exclude_korean_entertainment_news() {
        // Given
        ArticleDto article = createArticle(
            "K-POP 아이돌 그룹 새 앨범 발매",
            "연예계 스타들이 드라마 촬영을 위해 모였다"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertEquals(0, score.getScore());
        assertEquals(NewsCategory.EXCLUDED, score.getCategory());
    }

    @Test
    void should_give_medium_score_for_corporate_news() {
        // Given
        ArticleDto article = createArticle(
            "Major merger announcement between two corporations",
            "The acquisition deal is expected to create significant value for shareholders"
        );

        // When
        NewsQualityScore score = classifier.evaluateNewsQuality(article);

        // Then
        assertTrue(score.getScore() >= 2, "Corporate news should get medium score");
        assertTrue(score.getCategory() == NewsCategory.MEDIUM_PRIORITY || 
                  score.getCategory() == NewsCategory.HIGH_PRIORITY);
    }

    @Test
    void should_filter_based_on_minimum_score() {
        // Given
        ArticleDto lowQualityArticle = createArticle("Random business news", "Some general business information");
        ArticleDto highQualityArticle = createArticle("Stock market crashes on inflation fears", "Major indices fall as Fed policy changes");

        // When
        boolean shouldIncludeLow = classifier.shouldIncludeNews(lowQualityArticle, 3);
        boolean shouldIncludeHigh = classifier.shouldIncludeNews(highQualityArticle, 3);

        // Then
        assertFalse(shouldIncludeLow, "Low quality article should be filtered out");
        assertTrue(shouldIncludeHigh, "High quality article should be included");
    }

    private ArticleDto createArticle(String title, String description) {
        ArticleDto article = new ArticleDto();
        article.setTitle(title);
        article.setDescription(description);
        article.setSource("Test Source");
        article.setUrl("http://test.com/article");
        article.setPublishedAt(LocalDateTime.now());
        return article;
    }
}