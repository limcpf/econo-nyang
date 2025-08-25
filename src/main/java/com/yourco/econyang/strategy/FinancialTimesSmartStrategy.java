package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.ArticleDateCacheService;
import com.yourco.econyang.util.UrlDateExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Financial Times 전용 스마트 날짜 필터링 전략
 */
@Component
public class FinancialTimesSmartStrategy implements SmartDateFilterStrategy {
    
    @Autowired
    private ArticleDateCacheService cacheService;
    
    @Override
    public boolean supports(String rssSourceCode) {
        return rssSourceCode != null && rssSourceCode.toLowerCase().contains("ft");
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() != null) {
            return article.getPublishedAt().isAfter(getCutoffDateTime(rssSourceCode));
        }
        
        // 발행일이 없으면 스마트 추정으로 판단
        ArticleDto articleDto = convertToDto(article);
        DateEstimationResult result = estimateDateWithFallbackChain(articleDto, 0, 1, rssSourceCode);
        
        return result.isValid() && 
               result.getEstimatedDate().get().isAfter(getCutoffDateTime(rssSourceCode));
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return 72; // Financial Times는 72시간
    }
    
    @Override
    public String getStrategyName() {
        return "FinancialTimesSmartStrategy";
    }
    
    @Override
    public Optional<LocalDateTime> extractDateFromUrl(String url, String rssSourceCode) {
        if (url == null || !url.contains("ft.com")) {
            return Optional.empty();
        }
        
        // FT URL 패턴들
        // 1. /content/uuid 형식이 일반적이라 URL에서 날짜 추출은 어려움
        // 2. 대신 일반적인 패턴 시도
        return UrlDateExtractor.extractDateFromUrlForSource(url, "Financial Times");
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromRssPosition(int rssPosition, int totalArticles, String rssSourceCode) {
        // FT는 최신 기사가 맨 위에 오는 경향
        LocalDateTime now = LocalDateTime.now();
        
        if (rssPosition == 0) {
            // 첫 번째 기사는 매우 최근 (1시간 이내)
            return Optional.of(now.minusMinutes(30));
        } else if (rssPosition < 5) {
            // 상위 5개는 12시간 이내
            return Optional.of(now.minusHours(2 + rssPosition * 2));
        } else if (rssPosition < 10) {
            // 상위 10개는 24시간 이내  
            return Optional.of(now.minusHours(12 + (rssPosition - 5) * 2));
        } else {
            // 나머지는 48시간 이내
            return Optional.of(now.minusHours(24 + (rssPosition - 10)));
        }
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromPublishingPattern(ArticleDto articleDto, String rssSourceCode) {
        // FT는 주로 런던 시간대 기준 6시-24시 사이에 발행
        LocalDateTime now = LocalDateTime.now();
        
        // 현재 시간이 발행 패턴에 맞으면 최근 기사로 추정
        int currentHour = now.getHour();
        if (currentHour >= 6 && currentHour <= 23) {
            // 활발한 발행 시간대
            return Optional.of(now.minusHours(1));
        } else {
            // 비활성 시간대 - 이전 발행 시간대 마지막으로 추정
            if (currentHour < 6) {
                return Optional.of(now.withHour(23).minusDays(1));
            } else {
                return Optional.of(now.withHour(23));
            }
        }
    }
    
    @Override
    public double calculateConfidenceScore(LocalDateTime estimatedDate, String extractionMethod, String rssSourceCode) {
        double baseScore = 0.5;
        
        switch (extractionMethod) {
            case "url":
                baseScore = 0.8; // URL에서 추출한 경우 높은 신뢰도
                break;
            case "rss_position":
                baseScore = 0.6; // RSS 순서 기반은 중간 신뢰도
                break;
            case "pattern":
                baseScore = 0.4; // 패턴 기반은 낮은 신뢰도
                break;
            case "cache":
                baseScore = 0.9; // 캐시된 결과는 매우 높은 신뢰도
                break;
        }
        
        // 시간 기반 신뢰도 조정
        LocalDateTime now = LocalDateTime.now();
        long hoursDiff = java.time.Duration.between(estimatedDate, now).toHours();
        
        if (hoursDiff < 0) {
            // 미래 날짜는 신뢰도 낮음
            baseScore *= 0.3;
        } else if (hoursDiff <= 24) {
            // 24시간 이내는 신뢰도 높음
            baseScore *= 1.0;
        } else if (hoursDiff <= 72) {
            // 72시간 이내는 중간
            baseScore *= 0.8;
        } else {
            // 그 이상은 낮음
            baseScore *= 0.5;
        }
        
        return Math.min(1.0, Math.max(0.0, baseScore));
    }
    
    @Override
    public DateEstimationResult estimateDateWithFallbackChain(ArticleDto articleDto, int rssPosition, 
                                                             int totalArticles, String rssSourceCode) {
        
        // 1. 캐시 확인
        Optional<DateEstimationResult> cached = cacheService.getCachedDate(articleDto.getUrl(), "Financial Times");
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. URL에서 날짜 추출 시도
        Optional<LocalDateTime> urlDate = extractDateFromUrl(articleDto.getUrl(), rssSourceCode);
        if (urlDate.isPresent()) {
            double confidence = calculateConfidenceScore(urlDate.get(), "url", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(urlDate, confidence, "url", 
                    "URL 패턴에서 추출: " + articleDto.getUrl());
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "Financial Times", result);
                return result;
            }
        }
        
        // 3. RSS 순서 기반 추정
        if (assumeRecentArticlesValid(rssSourceCode) && rssPosition < getAssumedRecentArticleCount(rssSourceCode)) {
            Optional<LocalDateTime> positionDate = estimateDateFromRssPosition(rssPosition, totalArticles, rssSourceCode);
            if (positionDate.isPresent()) {
                double confidence = calculateConfidenceScore(positionDate.get(), "rss_position", rssSourceCode);
                DateEstimationResult result = new DateEstimationResult(positionDate, confidence, "rss_position",
                        String.format("RSS 순서 기반 추정: %d/%d", rssPosition, totalArticles));
                
                if (result.isValid()) {
                    cacheService.saveDateExtractionResult(articleDto.getUrl(), "Financial Times", result);
                    return result;
                }
            }
        }
        
        // 4. 발행 패턴 기반 추정
        Optional<LocalDateTime> patternDate = estimateDateFromPublishingPattern(articleDto, rssSourceCode);
        if (patternDate.isPresent()) {
            double confidence = calculateConfidenceScore(patternDate.get(), "pattern", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(patternDate, confidence, "pattern",
                    "발행 패턴 기반 추정");
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "Financial Times", result);
                return result;
            }
        }
        
        // 모든 방법 실패
        return new DateEstimationResult(Optional.empty(), 0.0, "failed", "모든 추정 방법 실패");
    }
    
    @Override
    public String getTypicalPublishingHours(String rssSourceCode) {
        return "6-23"; // FT는 런던 시간 기준 6시-23시
    }
    
    @Override
    public int getAverageArticlesPerDay(String rssSourceCode) {
        return 30; // FT는 하루 평균 30개 정도
    }
    
    @Override
    public int getAssumedRecentArticleCount(String rssSourceCode) {
        return 5; // 상위 5개는 최신으로 간주
    }
    
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setSource("Financial Times");
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }
}