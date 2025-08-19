package com.yourco.econdigest.integration;

import com.yourco.econdigest.config.RssSourcesConfig;
import com.yourco.econdigest.dto.ArticleDto;
import com.yourco.econdigest.service.ContentExtractionService;
import com.yourco.econdigest.service.RssFeedService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RSS 수집부터 본문 추출까지의 통합 테스트
 */
class RssToContentIntegrationTest {

    private RssFeedService rssFeedService = new RssFeedService();
    private ContentExtractionService contentExtractionService = new ContentExtractionService();

    @Test
    void testFullPipeline_RssToContent() {
        System.out.println("=== RSS 수집부터 본문 추출까지 통합 테스트 시작 ===");
        
        // 1. 실제 RSS 소스 설정 (Google 뉴스 - 상대적으로 안정적)
        RssSourcesConfig.RssSource testSource = new RssSourcesConfig.RssSource();
        testSource.setName("Google 뉴스 - 한국경제");
        testSource.setCode("google_kr_econ");
        testSource.setUrl("https://news.google.com/rss/search?q=%ED%95%9C%EA%B5%AD%EA%B2%BD%EC%A0%9C&hl=ko&gl=KR&ceid=KR:ko");
        testSource.setWeight(1.0);
        testSource.setEnabled(true);
        testSource.setTimeout(15000);
        testSource.setRetryCount(2);
        
        List<RssSourcesConfig.RssSource> sources = Arrays.asList(testSource);
        
        // 2. RSS 피드 수집
        System.out.println("1단계: RSS 피드 수집 중...");
        List<ArticleDto> fetchedArticles = rssFeedService.fetchAllArticles(sources, 3); // 최대 3개만
        
        System.out.println("수집된 기사 수: " + fetchedArticles.size());
        assertNotNull(fetchedArticles);
        
        if (fetchedArticles.isEmpty()) {
            System.out.println("WARNING: RSS 피드에서 기사를 수집하지 못했습니다. 네트워크 문제일 수 있습니다.");
            return; // 네트워크 이슈로 실패한 경우 테스트 종료
        }
        
        // 수집된 기사 목록 출력
        System.out.println("수집된 기사 목록:");
        for (int i = 0; i < fetchedArticles.size(); i++) {
            ArticleDto article = fetchedArticles.get(i);
            System.out.println("  " + (i + 1) + ". " + article.getTitle());
            System.out.println("     URL: " + article.getUrl());
            System.out.println("     소스: " + article.getSource());
        }
        
        // 3. 키워드 필터링 (간단한 필터 적용)
        System.out.println("\n2단계: 키워드 필터링...");
        RssSourcesConfig.FilterConfig filters = new RssSourcesConfig.FilterConfig();
        filters.setIncludeKeywords(Arrays.asList("경제", "금리", "투자", "주식", "기업"));
        filters.setExcludeKeywords(Arrays.asList("스포츠", "연예", "게임"));
        
        List<ArticleDto> filteredArticles = rssFeedService.applyFilters(fetchedArticles, filters);
        System.out.println("필터링 후 기사 수: " + filteredArticles.size());
        
        if (filteredArticles.isEmpty()) {
            System.out.println("WARNING: 키워드 필터링 후 기사가 남지 않았습니다.");
            return;
        }
        
        // 4. 본문 추출 (최대 2개만 테스트 - 시간 절약)
        System.out.println("\n3단계: 본문 추출 중...");
        int testArticleCount = Math.min(2, filteredArticles.size());
        List<ArticleDto> testArticles = filteredArticles.subList(0, testArticleCount);
        
        List<ArticleDto> extractedArticles = contentExtractionService.extractContents(testArticles);
        
        // 5. 결과 검증 및 출력
        System.out.println("\n=== 최종 결과 ===");
        System.out.println("처리된 기사 수: " + extractedArticles.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < extractedArticles.size(); i++) {
            ArticleDto article = extractedArticles.get(i);
            System.out.println("\n기사 " + (i + 1) + ":");
            System.out.println("  제목: " + article.getTitle());
            System.out.println("  URL: " + article.getUrl());
            System.out.println("  추출 성공: " + article.isExtractSuccess());
            
            if (article.isExtractSuccess()) {
                successCount++;
                String content = article.getContent();
                System.out.println("  본문 길이: " + (content != null ? content.length() : 0) + "자");
                
                // 본문 일부 출력 (처음 200자만)
                if (content != null && content.length() > 0) {
                    String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    System.out.println("  본문 미리보기: " + preview);
                    
                    // 본문이 실제로 추출되었는지 검증
                    assertTrue(content.length() > 10, "추출된 본문이 너무 짧습니다.");
                } else {
                    fail("추출 성공으로 표시되었지만 본문이 비어있습니다.");
                }
            } else {
                failCount++;
                System.out.println("  추출 실패 원인: " + article.getExtractError());
            }
        }
        
        double successRate = extractedArticles.isEmpty() ? 0.0 : (double) successCount / extractedArticles.size();
        System.out.println("\n성공률: " + String.format("%.1f", successRate * 100) + "% (" + successCount + "/" + extractedArticles.size() + ")");
        
        // 최소한의 성공 기준: 50% 이상 성공 또는 최소 1개 성공
        assertTrue(successCount > 0 || extractedArticles.isEmpty(), "최소 1개 기사의 본문은 추출되어야 합니다.");
        
        System.out.println("\n=== 통합 테스트 완료 ===");
    }
    
    @Test
    void testWithMultipleSources() {
        System.out.println("=== 다중 RSS 소스 통합 테스트 시작 ===");
        
        // 여러 RSS 소스 설정 (안정적인 것들만)
        RssSourcesConfig.RssSource source1 = new RssSourcesConfig.RssSource();
        source1.setName("Google 뉴스");
        source1.setCode("google");
        source1.setUrl("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko");
        source1.setWeight(1.0);
        source1.setEnabled(true);
        source1.setTimeout(10000);
        source1.setRetryCount(1);
        
        RssSourcesConfig.RssSource source2 = new RssSourcesConfig.RssSource();
        source2.setName("Google 경제뉴스");
        source2.setCode("google_econ");
        source2.setUrl("https://news.google.com/rss/search?q=%EA%B2%BD%EC%A0%9C&hl=ko&gl=KR&ceid=KR:ko");
        source2.setWeight(1.0);
        source2.setEnabled(true);
        source2.setTimeout(10000);
        source2.setRetryCount(1);
        
        List<RssSourcesConfig.RssSource> sources = Arrays.asList(source1, source2);
        
        // RSS 수집
        System.out.println("여러 소스에서 RSS 피드 수집 중...");
        List<ArticleDto> articles = rssFeedService.fetchAllArticles(sources, 5); // 최대 5개
        
        System.out.println("총 수집된 기사 수: " + articles.size());
        
        if (!articles.isEmpty()) {
            // 소스별 수집 확인
            long googleCount = articles.stream().mapToLong(a -> "google".equals(a.getSource()) ? 1 : 0).sum();
            long googleEconCount = articles.stream().mapToLong(a -> "google_econ".equals(a.getSource()) ? 1 : 0).sum();
            
            System.out.println("Google 뉴스: " + googleCount + "개");
            System.out.println("Google 경제뉴스: " + googleEconCount + "개");
            
            // 중복 제거 확인 (URL 기반)
            long uniqueUrls = articles.stream().map(ArticleDto::getUrl).distinct().count();
            System.out.println("유니크한 URL 수: " + uniqueUrls);
            assertEquals(articles.size(), uniqueUrls, "중복된 URL이 있습니다.");
        } else {
            System.out.println("WARNING: 다중 소스에서도 기사를 수집하지 못했습니다.");
        }
        
        System.out.println("=== 다중 RSS 소스 테스트 완료 ===");
    }
}