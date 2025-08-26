package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.strategy.SmartDateFilterStrategy;
import com.yourco.econyang.strategy.SmartDateFilterStrategy.DateEstimationResult;
import com.yourco.econyang.strategy.UniversalSmartStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 스마트 날짜 필터링 통합 서비스
 * 여러 SmartStrategy를 관리하고 폴백 체인을 통해 최적의 날짜 추정 제공
 */
@Service
public class SmartDateFilterService {
    
    @Autowired
    private List<SmartDateFilterStrategy> smartStrategies;
    
    @Autowired
    private ArticleDateCacheService cacheService;
    
    @Autowired
    private UniversalSmartStrategy universalSmartStrategy;
    
    @Autowired
    private com.yourco.econyang.util.PerformanceMonitor performanceMonitor;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final Map<String, SmartDateFilterStrategy> strategyCache = new ConcurrentHashMap<>();
    
    /**
     * 발행일자가 없는 기사들을 스마트하게 필터링
     */
    public List<ArticleDto> filterArticlesWithSmartStrategies(List<ArticleDto> articlesWithoutDate, 
                                                             LocalDateTime cutoffTime) {
        
        System.out.println("=== 스마트 날짜 필터링 시작 ===");
        System.out.println("발행일자 없는 기사 수: " + articlesWithoutDate.size());
        System.out.println("기준 시간: " + cutoffTime);
        
        // 언론사별로 그룹핑
        Map<String, List<ArticleDto>> articlesBySource = articlesWithoutDate.stream()
                .collect(Collectors.groupingBy(ArticleDto::getSource));
        
        System.out.println("언론사별 그룹: " + articlesBySource.keySet());
        
        List<CompletableFuture<List<ArticleDto>>> futures = articlesBySource.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> 
                    processArticlesBySource(entry.getKey(), entry.getValue(), cutoffTime), executorService))
                .collect(Collectors.toList());
        
        // 모든 병렬 작업 완료 대기 및 결과 수집
        List<ArticleDto> validArticles = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        System.err.println("병렬 처리 오류: " + e.getMessage());
                        return List.<ArticleDto>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        System.out.println("스마트 필터링 완료: " + validArticles.size() + "개 기사가 24시간 이내로 확인됨");
        return validArticles;
    }
    
    /**
     * 특정 언론사의 기사들을 스마트 전략으로 처리
     */
    private List<ArticleDto> processArticlesBySource(String sourceName, List<ArticleDto> articles, 
                                                   LocalDateTime cutoffTime) {
        
        System.out.println(sourceName + ": " + articles.size() + "개 기사 스마트 처리 시작");
        
        SmartDateFilterStrategy strategy = getStrategyForSource(sourceName);
        if (strategy == null) {
            // 전용 전략이 없으면 UniversalSmartStrategy 사용
            strategy = universalSmartStrategy;
            System.out.println(sourceName + ": 전용 전략 없음, 범용 스마트 전략 적용");
        }
        
        System.out.println(sourceName + ": " + strategy.getStrategyName() + " 전략 적용");
        
        final SmartDateFilterStrategy finalStrategy = strategy;
        List<ArticleDto> validArticles = articles.stream()
                .filter(article -> isArticleValid(article, finalStrategy, articles.indexOf(article), articles.size(), cutoffTime))
                .collect(Collectors.toList());
        
        System.out.println(sourceName + ": " + validArticles.size() + "개 유효 기사 확인");
        return validArticles;
    }
    
    /**
     * 개별 기사의 유효성 검사 (스마트 전략 사용)
     */
    private boolean isArticleValid(ArticleDto article, SmartDateFilterStrategy strategy, 
                                 int rssPosition, int totalArticles, LocalDateTime cutoffTime) {
        
        // 스마트 전략으로 날짜 추정
        DateEstimationResult result = strategy.estimateDateWithFallbackChain(
                article, rssPosition, totalArticles, getSourceCodeFromName(article.getSource()));
        
        if (!result.isValid()) {
            System.out.println(String.format("[%s] %s - 날짜 추정 실패: %s", 
                    article.getSource(), truncateTitle(article.getTitle()), result.getDetails()));
            return false;
        }
        
        boolean isRecent = result.getEstimatedDate().get().isAfter(cutoffTime);
        
        if (isRecent) {
            // 유효한 기사의 경우 발행일자 설정
            article.setPublishedAt(result.getEstimatedDate().get());
            System.out.println(String.format("[%s] ✓ %s - %s (신뢰도: %.2f, 방법: %s)", 
                    article.getSource(), truncateTitle(article.getTitle()), 
                    result.getEstimatedDate().get(), result.getConfidenceScore(), result.getExtractionMethod()));
        } else {
            System.out.println(String.format("[%s] ✗ %s - 오래된 기사: %s (신뢰도: %.2f)", 
                    article.getSource(), truncateTitle(article.getTitle()), 
                    result.getEstimatedDate().get(), result.getConfidenceScore()));
        }
        
        return isRecent;
    }
    
    /**
     * 언론사명에 맞는 스마트 전략 찾기
     */
    private SmartDateFilterStrategy getStrategyForSource(String sourceName) {
        if (sourceName == null) return null;
        
        // 캐시에서 먼저 확인
        SmartDateFilterStrategy cached = strategyCache.get(sourceName);
        if (cached != null) return cached;
        
        // 적합한 전략 찾기 (UniversalSmartStrategy 제외)
        String sourceCode = getSourceCodeFromName(sourceName);
        for (SmartDateFilterStrategy strategy : smartStrategies) {
            // UniversalSmartStrategy는 마지막 폴백으로 사용하므로 여기서 제외
            if (!(strategy instanceof UniversalSmartStrategy) && strategy.supports(sourceCode)) {
                strategyCache.put(sourceName, strategy);
                return strategy;
            }
        }
        
        // 전용 전략이 없으면 null 반환 (UniversalSmartStrategy가 폴백으로 사용됨)
        return null;
    }
    
    /**
     * 언론사명을 RSS 소스 코드로 변환
     */
    private String getSourceCodeFromName(String sourceName) {
        if (sourceName == null) return null;
        
        String lower = sourceName.toLowerCase();
        if (lower.contains("financial times")) return "ft_companies";
        if (lower.contains("bloomberg")) return "bloomberg_economics";
        if (lower.contains("marketwatch")) return "marketwatch";
        if (lower.contains("매일경제")) return "maeil_securities";
        if (lower.contains("investing")) return "investing_news";
        if (lower.contains("bbc")) return "bbc_business";
        
        return sourceName.toLowerCase().replaceAll("[\\s-]", "_");
    }
    
    
    /**
     * 캐시 및 전략 성능 통계 출력
     */
    public void printPerformanceStats() {
        System.out.println("=== 스마트 날짜 필터링 성능 통계 ===");
        
        // 전략별 통계
        for (SmartDateFilterStrategy strategy : smartStrategies) {
            System.out.println("- " + strategy.getStrategyName() + ": 활성화됨");
        }
        
        // 캐시 통계
        cacheService.printCacheStats();
        
        // 전략 캐시 크기
        System.out.println("전략 캐시 크기: " + strategyCache.size());
        
        // 성능 모니터 통계
        performanceMonitor.printPerformanceStats();
    }
    
    /**
     * 정리 작업 (캐시 정리, 스레드 풀 정리 등)
     */
    public void cleanup() {
        // 오래된 캐시 정리
        cacheService.cleanupOldCache();
        
        // 낮은 신뢰도 캐시 무효화
        cacheService.invalidateLowConfidenceCache(0.3);
        
        // 스레드 풀 정리
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        System.out.println("스마트 날짜 필터링 서비스 정리 완료");
    }
    
    /**
     * 개발/디버깅용 언론사별 상세 통계
     */
    public void printDetailedStats() {
        String[] sources = {"Financial Times", "Bloomberg Economics", "MarketWatch", "매일경제", "Investing.com", "KOTRA", "BBC Business"};
        
        System.out.println("=== 언론사별 상세 통계 ===");
        for (String source : sources) {
            SmartDateFilterStrategy strategy = getStrategyForSource(source);
            if (strategy == null) {
                strategy = universalSmartStrategy; // 폴백
            }
            
            System.out.println(String.format("\n[%s] - %s", source, strategy.getStrategyName()));
            System.out.println("  발행 시간대: " + strategy.getTypicalPublishingHours(getSourceCodeFromName(source)));
            System.out.println("  일평균 기사수: " + strategy.getAverageArticlesPerDay(getSourceCodeFromName(source)));
            System.out.println("  최신 기사 개수: " + strategy.getAssumedRecentArticleCount(getSourceCodeFromName(source)));
            
            cacheService.printExtractionStats(source);
        }
    }
    
    // Utility methods
    private String truncateTitle(String title) {
        if (title == null) return "제목없음";
        return title.length() > 50 ? title.substring(0, 47) + "..." : title;
    }
}