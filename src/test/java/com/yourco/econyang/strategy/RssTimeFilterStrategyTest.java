package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSS 시간 필터링 전략 테스트
 */
@ExtendWith(MockitoExtension.class)
class RssTimeFilterStrategyTest {

    private DefaultRssTimeFilterStrategy defaultStrategy;
    private InvestingComTimeFilterStrategy investingStrategy;
    private BbcTimeFilterStrategy bbcStrategy;
    
    @BeforeEach
    void setUp() {
        defaultStrategy = new DefaultRssTimeFilterStrategy();
        ReflectionTestUtils.setField(defaultStrategy, "defaultMaxAgeHours", 24);
        ReflectionTestUtils.setField(defaultStrategy, "enableTimeFilter", true);
        
        investingStrategy = new InvestingComTimeFilterStrategy();
        bbcStrategy = new BbcTimeFilterStrategy();
    }

    @Test
    void testDefaultStrategy_RecentArticle() {
        // Given
        Article recentArticle = new Article("test", "http://test.com", "Test Title");
        recentArticle.setPublishedAt(LocalDateTime.now().minusHours(12)); // 12시간 전
        
        // When
        boolean shouldInclude = defaultStrategy.shouldInclude(recentArticle, "test");
        
        // Then
        assertTrue(shouldInclude);
        assertEquals(24, defaultStrategy.getMaxAgeHours("test"));
    }
    
    @Test
    void testDefaultStrategy_OldArticle() {
        // Given
        Article oldArticle = new Article("test", "http://test.com", "Old Title");
        oldArticle.setPublishedAt(LocalDateTime.now().minusHours(30)); // 30시간 전
        
        // When
        boolean shouldInclude = defaultStrategy.shouldInclude(oldArticle, "test");
        
        // Then
        assertFalse(shouldInclude);
    }
    
    @Test
    void testInvestingComStrategy_NewsSource() {
        // Given
        Article article = new Article("investing_news", "http://test.com", "Market News");
        article.setPublishedAt(LocalDateTime.now().minusHours(8)); // 8시간 전
        
        // When
        boolean shouldInclude = investingStrategy.shouldInclude(article, "investing_news");
        
        // Then
        assertTrue(shouldInclude);
        assertEquals(12, investingStrategy.getMaxAgeHours("investing_news"));
    }
    
    @Test
    void testInvestingComStrategy_MarketSource_TooOld() {
        // Given
        Article article = new Article("investing_market", "http://test.com", "Market Overview");
        article.setPublishedAt(LocalDateTime.now().minusHours(8)); // 8시간 전
        
        // When
        boolean shouldInclude = investingStrategy.shouldInclude(article, "investing_market");
        
        // Then
        assertFalse(shouldInclude); // 6시간 기준이므로 8시간은 너무 오래됨
        assertEquals(6, investingStrategy.getMaxAgeHours("investing_market"));
    }
    
    @Test
    void testBbcStrategy_LongerTimeframe() {
        // Given
        Article article = new Article("bbc_business", "http://test.com", "BBC Analysis");
        article.setPublishedAt(LocalDateTime.now().minusHours(30)); // 30시간 전
        
        // When
        boolean shouldInclude = bbcStrategy.shouldInclude(article, "bbc_business");
        
        // Then
        assertTrue(shouldInclude); // BBC는 48시간이므로 30시간은 포함
        assertEquals(48, bbcStrategy.getMaxAgeHours("bbc_business"));
    }
    
    @Test
    void testStrategy_Supports() {
        // When & Then
        assertTrue(defaultStrategy.supports("any_source"));
        assertTrue(investingStrategy.supports("investing_news"));
        assertTrue(investingStrategy.supports("investing_market"));
        assertFalse(investingStrategy.supports("bbc_business"));
        assertTrue(bbcStrategy.supports("bbc_business"));
        assertFalse(bbcStrategy.supports("investing_news"));
    }
    
    @Test
    void testStrategy_NullPublishedDate() {
        // Given
        Article articleWithoutDate = new Article("test", "http://test.com", "No Date");
        articleWithoutDate.setPublishedAt(null);
        
        // When & Then
        assertTrue(defaultStrategy.shouldInclude(articleWithoutDate, "test"));
        assertTrue(investingStrategy.shouldInclude(articleWithoutDate, "investing_news"));
        assertTrue(bbcStrategy.shouldInclude(articleWithoutDate, "bbc_business"));
    }
    
    @Test
    void testStrategyNames() {
        assertEquals("DefaultRssTimeFilter", defaultStrategy.getStrategyName());
        assertEquals("InvestingComTimeFilter", investingStrategy.getStrategyName());
        assertEquals("BbcTimeFilter", bbcStrategy.getStrategyName());
    }
}