package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.ArticleDateCacheService;
import com.yourco.econyang.service.ContentDateExtractor;
import com.yourco.econyang.util.PerformanceMonitor;
import com.yourco.econyang.util.UrlDateExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 모든 RSS 소스를 지원하는 범용 스마트 날짜 필터링 전략
 * 처리 순서: RSS 메타데이터 → URL 분석 → 본문 스캔 (폴백 체인)
 */
@Component
public class UniversalSmartStrategy implements SmartDateFilterStrategy {
    
    @Autowired
    private ArticleDateCacheService cacheService;
    
    @Autowired
    private ContentDateExtractor contentDateExtractor;
    
    @Autowired
    private PerformanceMonitor performanceMonitor;
    
    @Value("${app.rss.enableContentScan:true}")
    private boolean enableContentScan;
    
    @Value("${app.rss.contentScanTimeout:10}")
    private int contentScanTimeoutSeconds;
    
    @Value("${app.rss.universalMaxAgeHours:72}")
    private int universalMaxAgeHours;
    
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, String> contentCache = new ConcurrentHashMap<>();
    
    public UniversalSmartStrategy() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    @Override
    public boolean supports(String rssSourceCode) {
        // 모든 RSS 소스 지원 (다른 특화 전략이 없는 경우 사용)
        return true;
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        // 1. RSS 메타데이터에 발행일이 있으면 일반적인 시간 필터링
        if (article.getPublishedAt() != null) {
            return article.getPublishedAt().isAfter(getCutoffDateTime(rssSourceCode));
        }
        
        // 2. 폴백 체인으로 날짜 추정 시도
        ArticleDto articleDto = convertToDto(article);
        DateEstimationResult result = estimateDateWithFallbackChain(articleDto, 0, 1, rssSourceCode);
        
        if (result.isValid()) {
            LocalDateTime estimatedDate = result.getEstimatedDate().get();
            boolean shouldInclude = estimatedDate.isAfter(getCutoffDateTime(rssSourceCode));
            
            if (shouldInclude) {
                System.out.println(String.format(
                    "✅ 범용 전략으로 날짜 추정 성공 [%s]: %s (추정일: %s, 방법: %s)",
                    rssSourceCode,
                    article.getTitle(),
                    estimatedDate,
                    result.getExtractionMethod()
                ));
                
                // 추정된 날짜를 Article에 설정 (옵션)
                article.setPublishedAt(estimatedDate);
            } else {
                System.out.println(String.format(
                    "❌ 범용 전략 날짜 추정 성공하나 기준 미달 [%s]: %s (추정일: %s, 기준: %s)",
                    rssSourceCode,
                    article.getTitle(),
                    estimatedDate,
                    getCutoffDateTime(rssSourceCode)
                ));
            }
            
            return shouldInclude;
        }
        
        // 3. 모든 방법 실패 시 일부 허용 (최후 수단)
        System.out.println(String.format(
            "⚠️ 범용 전략 날짜 추정 실패, 제한적 허용 고려 [%s]: %s",
            rssSourceCode,
            article.getTitle()
        ));
        
        return shouldFallbackInclude(rssSourceCode);
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return universalMaxAgeHours;
    }
    
    @Override
    public String getStrategyName() {
        return "UniversalSmartStrategy";
    }
    
    /**
     * 폴백 체인으로 날짜 추정
     * 1. URL 패턴 분석
     * 2. 본문 스캔 (활성화된 경우)
     * 3. 캐시된 결과 활용
     */
    @Override
    public DateEstimationResult estimateDateWithFallbackChain(ArticleDto article, int currentAttempt, int maxAttempts, String rssSourceCode) {
        String url = article.getUrl();
        
        // 캐시에서 확인
        if (cacheService != null) {
            Optional<DateEstimationResult> cachedResult = cacheService.getCachedDate(url, "Universal");
            if (cachedResult.isPresent()) {
                return cachedResult.get();
            }
        }
        
        // 1. URL 패턴 분석 시도
        Optional<LocalDateTime> urlDate = extractDateFromUrl(url, rssSourceCode);
        if (urlDate.isPresent()) {
            LocalDateTime dateTime = urlDate.get();
            DateEstimationResult result = new DateEstimationResult(Optional.of(dateTime), 0.9, "url_pattern", "Universal URL pattern extraction: " + url);
            if (cacheService != null) {
                cacheService.saveDateExtractionResult(url, "Universal", result);
            }
            return result;
        }
        
        // 2. 본문 스캔 시도 (활성화된 경우만)
        if (enableContentScan && currentAttempt < maxAttempts) {
            Optional<LocalDateTime> contentDate = extractDateFromContent(url, article.getSource());
            if (contentDate.isPresent()) {
                LocalDateTime dateTime = contentDate.get();
                DateEstimationResult result = new DateEstimationResult(Optional.of(dateTime), 0.8, "content_scan", "Universal content scan extraction: " + url);
                if (cacheService != null) {
                    cacheService.saveDateExtractionResult(url, "Universal", result);
                }
                return result;
            }
        }
        
        return new DateEstimationResult(Optional.empty(), 0.0, "failed", "All universal extraction methods failed for: " + url);
    }
    
    @Override
    public Optional<LocalDateTime> extractDateFromUrl(String url, String rssSourceCode) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // 범용 URL 패턴 추출 시도
        return UrlDateExtractor.extractDateFromUrl(url);
    }
    
    /**
     * URL에서 본문을 가져와서 날짜 추출
     */
    private Optional<LocalDateTime> extractDateFromContent(String url, String sourceName) {
        if (!enableContentScan || url == null) {
            return Optional.empty();
        }
        
        long startTime = performanceMonitor.startTiming("content_scan_total");
        
        try {
            // 콘텐츠 캐시 확인
            String content = contentCache.get(url);
            if (content == null) {
                long fetchStart = performanceMonitor.startTiming("content_fetch");
                content = fetchContent(url);
                performanceMonitor.endTiming("content_fetch", fetchStart);
                
                if (content != null) {
                    contentCache.put(url, content);
                }
            }
            
            if (content != null) {
                long extractStart = performanceMonitor.startTiming("content_date_extract");
                Optional<LocalDateTime> result = contentDateExtractor.extractDateForSource(content, sourceName);
                performanceMonitor.endTiming("content_date_extract", extractStart);
                
                if (result.isPresent()) {
                    performanceMonitor.recordSuccess("content_scan_total");
                } else {
                    performanceMonitor.recordFailure("content_scan_total");
                }
                
                return result;
            }
            
            performanceMonitor.recordFailure("content_scan_total");
            return Optional.empty();
            
        } finally {
            performanceMonitor.endTiming("content_scan_total", startTime);
        }
    }
    
    /**
     * HTTP로 본문 내용 가져오기
     */
    private String fetchContent(String url) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(contentScanTimeoutSeconds))
                    .GET();
            
            // Investing.com URL에 특별한 헤더 설정
            if (url.contains("investing.com")) {
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                            .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .header("DNT", "1")
                            .header("Connection", "keep-alive")
                            .header("Upgrade-Insecure-Requests", "1")
                            .header("Sec-Fetch-Dest", "document")
                            .header("Sec-Fetch-Mode", "navigate")
                            .header("Sec-Fetch-Site", "none")
                            .header("Cache-Control", "max-age=0");
            } else {
                requestBuilder.header("User-Agent", "EconDigest-SmartFilter/1.0");
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("본문 가져오기 실패 - HTTP " + response.statusCode() + ": " + url);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("본문 가져오기 실패 - " + e.getMessage() + ": " + url);
            return null;
        } catch (Exception e) {
            System.out.println("본문 가져오기 예외 - " + e.getMessage() + ": " + url);
            return null;
        }
    }
    
    /**
     * 모든 날짜 추정 실패 시 일부 기사 허용 여부 결정
     * 소스별로 다른 정책 적용 가능
     */
    private boolean shouldFallbackInclude(String rssSourceCode) {
        // 신뢰할 만한 소스들은 일부 허용
        if (rssSourceCode != null) {
            String lowerCode = rssSourceCode.toLowerCase();
            if (lowerCode.contains("bbc") || lowerCode.contains("bloomberg") || 
                lowerCode.contains("ft") || lowerCode.contains("marketwatch")) {
                return Math.random() < 0.3; // 30% 확률로 허용
            }
            
            if (lowerCode.contains("investing") || lowerCode.contains("kotra")) {
                return Math.random() < 0.2; // 20% 확률로 허용
            }
        }
        
        return false; // 기본적으로 제외
    }
    
    /**
     * Article -> ArticleDto 변환
     */
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setTitle(article.getTitle());
        dto.setUrl(article.getUrl());
        dto.setSource(article.getSource());
        dto.setDescription(article.getRawExcerpt());
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }
    
    // Missing interface methods implementation
    @Override
    public double calculateConfidenceScore(LocalDateTime estimatedDate, String extractionMethod, String rssSourceCode) {
        double baseScore = 0.5;
        
        switch (extractionMethod) {
            case "url_pattern":
                baseScore = 0.7;
                break;
            case "content_scan":
                baseScore = 0.8;
                break;
            case "cache":
                baseScore = 0.9;
                break;
            default:
                baseScore = 0.5;
                break;
        }
        
        // Time-based adjustment
        LocalDateTime now = LocalDateTime.now();
        long hoursDiff = java.time.Duration.between(estimatedDate, now).toHours();
        
        if (hoursDiff < 0) {
            baseScore *= 0.2; // Future dates are suspicious
        } else if (hoursDiff <= universalMaxAgeHours) {
            baseScore *= 1.0; // Within expected range
        } else {
            baseScore *= 0.6; // Older than expected
        }
        
        return Math.min(1.0, Math.max(0.0, baseScore));
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromRssPosition(int rssPosition, int totalArticles, String rssSourceCode) {
        LocalDateTime now = LocalDateTime.now();
        
        // Universal estimation based on RSS position
        if (rssPosition == 0) {
            return Optional.of(now.minusHours(1)); // Most recent
        } else if (rssPosition < 3) {
            return Optional.of(now.minusHours(2 + rssPosition));
        } else if (rssPosition < 8) {
            return Optional.of(now.minusHours(4 + rssPosition));
        } else {
            return Optional.of(now.minusHours(12 + Math.min(rssPosition, 60))); // Cap at 72 hours
        }
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromPublishingPattern(ArticleDto articleDto, String rssSourceCode) {
        LocalDateTime now = LocalDateTime.now();
        
        // Basic pattern estimation - assume recent publication
        return Optional.of(now.minusHours(6));
    }
    
    @Override
    public String getTypicalPublishingHours(String rssSourceCode) {
        return "0-23"; // Universal - 24/7
    }
    
    @Override
    public int getAverageArticlesPerDay(String rssSourceCode) {
        return 15; // Universal average
    }
    
    @Override
    public int getAssumedRecentArticleCount(String rssSourceCode) {
        return 5; // Conservative estimate
    }
}