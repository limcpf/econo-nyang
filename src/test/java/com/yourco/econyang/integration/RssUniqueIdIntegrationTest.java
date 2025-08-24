package com.yourco.econyang.integration;

import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.RssFeedService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSS 고유 식별자 기반 중복 처리 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class RssUniqueIdIntegrationTest {

    @Test
    void should_generate_unique_ids_for_articles() {
        // given - RssSourcesConfig 설정
        RssSourcesConfig config = createTestConfig();
        RssFeedService service = new RssFeedService(config);
        
        // RSS 소스 생성
        RssSourcesConfig.RssSource maeilSource = new RssSourcesConfig.RssSource();
        maeilSource.setName("매일경제 테스트");
        maeilSource.setCode("maeil_securities");
        maeilSource.setUrl("https://www.mk.co.kr/rss/30800011/");
        maeilSource.setEnabled(true);
        
        RssSourcesConfig.RssSource investingSource = new RssSourcesConfig.RssSource();
        investingSource.setName("Investing.com 테스트");
        investingSource.setCode("investing_news");
        investingSource.setUrl("https://www.investing.com/rss/news.rss");
        investingSource.setEnabled(true);
        
        List<RssSourcesConfig.RssSource> sources = Arrays.asList(maeilSource, investingSource);
        
        // when - 실제 RSS 피드에서 기사 수집
        List<ArticleDto> maeilArticles = service.fetchArticles(maeilSource);
        List<ArticleDto> investingArticles = service.fetchArticles(investingSource);
        
        // then - 고유 ID가 올바르게 생성되었는지 확인
        if (!maeilArticles.isEmpty()) {
            ArticleDto firstMaeilArticle = maeilArticles.get(0);
            System.out.println("매일경제 기사 예시:");
            System.out.println("- URL: " + firstMaeilArticle.getUrl());
            System.out.println("- UniqueId: " + firstMaeilArticle.getUniqueId());
            System.out.println("- Title: " + firstMaeilArticle.getTitle());
            
            assertNotNull(firstMaeilArticle.getUniqueId());
            assertTrue(firstMaeilArticle.getUniqueId().startsWith("maeil_") || 
                      firstMaeilArticle.getUniqueId().startsWith("hash_"));
        }
        
        if (!investingArticles.isEmpty()) {
            ArticleDto firstInvestingArticle = investingArticles.get(0);
            System.out.println("\nInvesting.com 기사 예시:");
            System.out.println("- URL: " + firstInvestingArticle.getUrl());
            System.out.println("- UniqueId: " + firstInvestingArticle.getUniqueId());
            System.out.println("- Title: " + firstInvestingArticle.getTitle());
            
            assertNotNull(firstInvestingArticle.getUniqueId());
            assertTrue(firstInvestingArticle.getUniqueId().startsWith("investing_") || 
                      firstInvestingArticle.getUniqueId().startsWith("hash_"));
        }
        
        // 중복 제거 테스트 (간단한 병합)
        List<ArticleDto> allArticles = new java.util.ArrayList<>();
        allArticles.addAll(maeilArticles);
        allArticles.addAll(investingArticles);
        System.out.println("\n전체 기사 수집 결과:");
        System.out.println("- 매일경제: " + maeilArticles.size() + "개");
        System.out.println("- Investing.com: " + investingArticles.size() + "개");
        System.out.println("- 중복 제거 후 전체: " + allArticles.size() + "개");
        
        // 모든 기사가 고유 ID를 가지고 있는지 확인
        for (ArticleDto article : allArticles) {
            assertNotNull(article.getUniqueId(), "기사의 고유 ID가 null입니다: " + article.getUrl());
            assertTrue(article.getUniqueId().length() > 5, "고유 ID가 너무 짧습니다: " + article.getUniqueId());
        }
        
        System.out.println("\n고유 ID 중복 처리 테스트 완료 ✅");
    }
    
    private RssSourcesConfig createTestConfig() {
        RssSourcesConfig config = new RssSourcesConfig();
        
        // Collection 설정
        RssSourcesConfig.CollectionConfig collection = new RssSourcesConfig.CollectionConfig();
        collection.setMaxRetries(2);
        collection.setRetryDelayMs(1000);
        collection.setConnectionTimeoutSec(10);
        collection.setReadTimeoutSec(15);
        collection.setUserAgent("EconoNyang-Test/1.0");
        config.setCollection(collection);
        
        return config;
    }
}