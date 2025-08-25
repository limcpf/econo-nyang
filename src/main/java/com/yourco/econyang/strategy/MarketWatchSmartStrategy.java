package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.ArticleDateCacheService;
import com.yourco.econyang.util.UrlDateExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarketWatch 전용 스마트 날짜 필터링 전략
 */
@Component
public class MarketWatchSmartStrategy implements SmartDateFilterStrategy {
    
    @Autowired
    private ArticleDateCacheService cacheService;
    
    // MarketWatch UUID 패턴 (종종 타임스탬프 포함)
    private static final Pattern MW_UUID_PATTERN = Pattern.compile(".*-([a-f0-9]{8})$");
    
    @Override
    public boolean supports(String rssSourceCode) {
        return rssSourceCode != null && rssSourceCode.toLowerCase().contains("marketwatch");
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() != null) {
            return article.getPublishedAt().isAfter(getCutoffDateTime(rssSourceCode));
        }
        
        ArticleDto articleDto = convertToDto(article);
        DateEstimationResult result = estimateDateWithFallbackChain(articleDto, 0, 1, rssSourceCode);
        
        return result.isValid() && 
               result.getEstimatedDate().get().isAfter(getCutoffDateTime(rssSourceCode));
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return 48; // MarketWatch는 48시간
    }
    
    @Override
    public String getStrategyName() {
        return "MarketWatchSmartStrategy";
    }
    
    @Override
    public Optional<LocalDateTime> extractDateFromUrl(String url, String rssSourceCode) {
        if (url == null || !url.contains("marketwatch.com")) {
            return Optional.empty();
        }
        
        // MarketWatch 특화 패턴들
        // 1. 기본적인 URL 패턴 시도
        Optional<LocalDateTime> basicDate = UrlDateExtractor.extractDateFromUrlForSource(url, "MarketWatch");
        if (basicDate.isPresent()) {
            return basicDate;
        }
        
        // 2. MarketWatch UUID에서 헥스 타임스탬프 추출 시도
        Matcher matcher = MW_UUID_PATTERN.matcher(url);
        if (matcher.find()) {
            String hexTimestamp = matcher.group(1);
            try {
                // 헥스를 long으로 변환해서 타임스탬프로 시도
                long timestamp = Long.parseLong(hexTimestamp, 16);
                
                // 합리적인 타임스탬프 범위인지 확인 (2020-2030년 사이)
                if (timestamp > 1577836800L && timestamp < 1893456000L) { // Unix timestamp 범위
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, 
                            java.time.ZoneOffset.systemDefault().getRules()
                                .getOffset(java.time.Instant.ofEpochSecond(timestamp)));
                    return Optional.of(dateTime);
                }
                
                // 밀리초 단위 타임스탬프 시도
                if (timestamp > 1577836800000L && timestamp < 1893456000000L) {
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0,
                            java.time.ZoneOffset.systemDefault().getRules()
                                .getOffset(java.time.Instant.ofEpochMilli(timestamp)));
                    return Optional.of(dateTime);
                }
                
            } catch (NumberFormatException e) {
                // 헥스 파싱 실패, 계속 진행
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromRssPosition(int rssPosition, int totalArticles, String rssSourceCode) {
        // MarketWatch는 미국 시장 시간에 맞춰 주로 발행
        LocalDateTime now = LocalDateTime.now();
        
        if (rssPosition == 0) {
            // 첫 번째 기사는 1시간 이내
            return Optional.of(now.minusMinutes(30));
        } else if (rssPosition < 5) {
            // 상위 5개는 8시간 이내
            return Optional.of(now.minusHours(1 + rssPosition));
        } else if (rssPosition < 12) {
            // 상위 12개는 24시간 이내
            return Optional.of(now.minusHours(6 + (rssPosition - 5) * 2));
        } else {
            // 나머지는 48시간 이내
            return Optional.of(now.minusHours(24 + Math.min(rssPosition - 12, 24)));
        }
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromPublishingPattern(ArticleDto articleDto, String rssSourceCode) {
        LocalDateTime now = LocalDateTime.now();
        
        // MarketWatch는 주로 미국 동부 시간대 기준 6시-20시 활발
        // 한국 시간으로는 20시-10시 (다음날)
        int currentHour = now.getHour();
        
        // 제목에서 시장 관련 키워드 확인
        String title = articleDto.getTitle();
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            
            if (lowerTitle.contains("stock") || lowerTitle.contains("market") || 
                lowerTitle.contains("trading") || lowerTitle.contains("dow") || 
                lowerTitle.contains("s&p") || lowerTitle.contains("nasdaq")) {
                
                // 시장 관련 기사는 미국 장 시간대에 맞춰 추정
                if (currentHour >= 20 || currentHour <= 10) {
                    // 미국 장 시간대 - 최근 기사로 추정
                    return Optional.of(now.minusHours(2));
                } else {
                    // 미국 장 마감 후 - 이전 장 시간대로 추정
                    return Optional.of(now.withHour(8)); // 미국 장 마감 시간
                }
            }
            
            if (lowerTitle.contains("earnings") || lowerTitle.contains("quarterly")) {
                // 실적 발표 관련은 보통 장 마감 후
                return Optional.of(now.withHour(9).minusMinutes(30));
            }
        }
        
        // 기본적으로 최근 6시간 이내로 추정
        return Optional.of(now.minusHours(6));
    }
    
    @Override
    public double calculateConfidenceScore(LocalDateTime estimatedDate, String extractionMethod, String rssSourceCode) {
        double baseScore = 0.5;
        
        switch (extractionMethod) {
            case "url":
                baseScore = 0.7; // MarketWatch URL 패턴은 중간 신뢰도
                break;
            case "rss_position":
                baseScore = 0.6; // RSS 순서 기반은 중간
                break;
            case "pattern":
                baseScore = 0.5; // 패턴 기반은 중간
                break;
            case "cache":
                baseScore = 0.9;
                break;
        }
        
        // 시간 기반 신뢰도 조정 (미국 시장 특성 고려)
        LocalDateTime now = LocalDateTime.now();
        long hoursDiff = java.time.Duration.between(estimatedDate, now).toHours();
        int currentHour = now.getHour();
        
        // 미국 장 시간대 (한국 시간 20-10시)에 발행된 기사는 신뢰도 높임
        int estimatedHour = estimatedDate.getHour();
        boolean isDuringUsMarketHours = (estimatedHour >= 20 || estimatedHour <= 10);
        
        if (isDuringUsMarketHours) {
            baseScore *= 1.1; // 10% 보너스
        }
        
        // 시간차 기반 조정
        if (hoursDiff < 0) {
            baseScore *= 0.2;
        } else if (hoursDiff <= 12) {
            baseScore *= 1.0;
        } else if (hoursDiff <= 48) {
            baseScore *= 0.8;
        } else {
            baseScore *= 0.4;
        }
        
        return Math.min(1.0, Math.max(0.0, baseScore));
    }
    
    @Override
    public DateEstimationResult estimateDateWithFallbackChain(ArticleDto articleDto, int rssPosition, 
                                                             int totalArticles, String rssSourceCode) {
        
        // 1. 캐시 확인
        Optional<DateEstimationResult> cached = cacheService.getCachedDate(articleDto.getUrl(), "MarketWatch");
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. URL에서 날짜 추출 시도 (UUID 패턴 포함)
        Optional<LocalDateTime> urlDate = extractDateFromUrl(articleDto.getUrl(), rssSourceCode);
        if (urlDate.isPresent()) {
            double confidence = calculateConfidenceScore(urlDate.get(), "url", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(urlDate, confidence, "url",
                    "MarketWatch URL 패턴에서 추출 (UUID 포함): " + articleDto.getUrl());
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "MarketWatch", result);
                return result;
            }
        }
        
        // 3. RSS 순서 기반 추정
        if (rssPosition < getAssumedRecentArticleCount(rssSourceCode)) {
            Optional<LocalDateTime> positionDate = estimateDateFromRssPosition(rssPosition, totalArticles, rssSourceCode);
            if (positionDate.isPresent()) {
                double confidence = calculateConfidenceScore(positionDate.get(), "rss_position", rssSourceCode);
                DateEstimationResult result = new DateEstimationResult(positionDate, confidence, "rss_position",
                        String.format("MarketWatch RSS 순서: %d/%d", rssPosition + 1, totalArticles));
                
                if (result.isValid()) {
                    cacheService.saveDateExtractionResult(articleDto.getUrl(), "MarketWatch", result);
                    return result;
                }
            }
        }
        
        // 4. 미국 시장 패턴 기반 추정
        Optional<LocalDateTime> patternDate = estimateDateFromPublishingPattern(articleDto, rssSourceCode);
        if (patternDate.isPresent()) {
            double confidence = calculateConfidenceScore(patternDate.get(), "pattern", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(patternDate, confidence, "pattern",
                    "MarketWatch 미국 시장 패턴 기반");
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "MarketWatch", result);
                return result;
            }
        }
        
        // 모든 방법 실패
        return new DateEstimationResult(Optional.empty(), 0.0, "failed", "모든 추정 방법 실패");
    }
    
    @Override
    public String getTypicalPublishingHours(String rssSourceCode) {
        return "20-10"; // 미국 장 시간대 (한국 시간 기준)
    }
    
    @Override
    public int getAverageArticlesPerDay(String rssSourceCode) {
        return 25; // MarketWatch는 하루 평균 25개
    }
    
    @Override
    public int getAssumedRecentArticleCount(String rssSourceCode) {
        return 6; // 상위 6개는 최신으로 간주
    }
    
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setSource("MarketWatch");
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }
}