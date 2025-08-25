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
 * Bloomberg 전용 스마트 날짜 필터링 전략
 */
@Component
public class BloombergSmartStrategy implements SmartDateFilterStrategy {
    
    @Autowired
    private ArticleDateCacheService cacheService;
    
    @Override
    public boolean supports(String rssSourceCode) {
        return rssSourceCode != null && rssSourceCode.toLowerCase().contains("bloomberg");
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
        return 48; // Bloomberg는 48시간 (더 빠른 뉴스 사이클)
    }
    
    @Override
    public String getStrategyName() {
        return "BloombergSmartStrategy";
    }
    
    @Override
    public Optional<LocalDateTime> extractDateFromUrl(String url, String rssSourceCode) {
        if (url == null || !url.contains("bloomberg.com")) {
            return Optional.empty();
        }
        
        // Bloomberg URL 특화 패턴들
        // 1. /news/articles/2025-08-25/article-title
        // 2. /news/articles/YYYY-MM-DD/article-id
        return UrlDateExtractor.extractDateFromUrlForSource(url, "Bloomberg");
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromRssPosition(int rssPosition, int totalArticles, String rssSourceCode) {
        // Bloomberg는 매우 빈번한 업데이트
        LocalDateTime now = LocalDateTime.now();
        
        if (rssPosition == 0) {
            // 첫 번째 기사는 매우 최근 (30분 이내)
            return Optional.of(now.minusMinutes(15));
        } else if (rssPosition < 3) {
            // 상위 3개는 2시간 이내
            return Optional.of(now.minusHours(rssPosition));
        } else if (rssPosition < 8) {
            // 상위 8개는 12시간 이내
            return Optional.of(now.minusHours(2 + (rssPosition - 3) * 2));
        } else if (rssPosition < 15) {
            // 상위 15개는 24시간 이내
            return Optional.of(now.minusHours(12 + (rssPosition - 8)));
        } else {
            // 나머지는 48시간 이내
            return Optional.of(now.minusHours(24 + Math.min(rssPosition - 15, 24)));
        }
    }
    
    @Override
    public Optional<LocalDateTime> estimateDateFromPublishingPattern(ArticleDto articleDto, String rssSourceCode) {
        // Bloomberg는 24시간 발행 (글로벌 뉴스)
        LocalDateTime now = LocalDateTime.now();
        
        // 제목이나 내용에서 시급성 키워드 확인
        String title = articleDto.getTitle();
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("breaking") || lowerTitle.contains("urgent") || 
                lowerTitle.contains("flash") || lowerTitle.contains("alert")) {
                // 속보성 기사는 매우 최근
                return Optional.of(now.minusMinutes(30));
            }
            
            if (lowerTitle.contains("preview") || lowerTitle.contains("outlook")) {
                // 전망/예고 기사는 비교적 최근
                return Optional.of(now.minusHours(2));
            }
        }
        
        // 기본적으로 최근 4시간 이내로 추정
        return Optional.of(now.minusHours(4));
    }
    
    @Override
    public double calculateConfidenceScore(LocalDateTime estimatedDate, String extractionMethod, String rssSourceCode) {
        double baseScore = 0.5;
        
        switch (extractionMethod) {
            case "url":
                baseScore = 0.9; // Bloomberg URL 패턴은 매우 신뢰도 높음
                break;
            case "rss_position":
                baseScore = 0.7; // RSS 순서도 비교적 신뢰도 높음 (빠른 업데이트)
                break;
            case "pattern":
                baseScore = 0.5; // 패턴 기반은 중간
                break;
            case "cache":
                baseScore = 0.95;
                break;
        }
        
        // Bloomberg는 실시간성이 중요하므로 시간 기반 신뢰도 조정 강화
        LocalDateTime now = LocalDateTime.now();
        long hoursDiff = java.time.Duration.between(estimatedDate, now).toHours();
        
        if (hoursDiff < 0) {
            baseScore *= 0.1; // 미래 날짜는 매우 낮은 신뢰도
        } else if (hoursDiff <= 6) {
            baseScore *= 1.0; // 6시간 이내는 최고 신뢰도
        } else if (hoursDiff <= 24) {
            baseScore *= 0.9; // 24시간 이내는 높은 신뢰도
        } else if (hoursDiff <= 48) {
            baseScore *= 0.7; // 48시간 이내는 중간
        } else {
            baseScore *= 0.3; // 그 이상은 매우 낮음
        }
        
        return Math.min(1.0, Math.max(0.0, baseScore));
    }
    
    @Override
    public DateEstimationResult estimateDateWithFallbackChain(ArticleDto articleDto, int rssPosition, 
                                                             int totalArticles, String rssSourceCode) {
        
        // 1. 캐시 확인
        Optional<DateEstimationResult> cached = cacheService.getCachedDate(articleDto.getUrl(), "Bloomberg Economics");
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. URL에서 날짜 추출 시도 (Bloomberg는 URL 패턴이 좋음)
        Optional<LocalDateTime> urlDate = extractDateFromUrl(articleDto.getUrl(), rssSourceCode);
        if (urlDate.isPresent()) {
            double confidence = calculateConfidenceScore(urlDate.get(), "url", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(urlDate, confidence, "url",
                    "Bloomberg URL 패턴에서 추출: " + articleDto.getUrl());
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "Bloomberg Economics", result);
                return result;
            }
        }
        
        // 3. RSS 순서 기반 추정 (Bloomberg는 매우 신뢰도 높음)
        if (rssPosition < getAssumedRecentArticleCount(rssSourceCode)) {
            Optional<LocalDateTime> positionDate = estimateDateFromRssPosition(rssPosition, totalArticles, rssSourceCode);
            if (positionDate.isPresent()) {
                double confidence = calculateConfidenceScore(positionDate.get(), "rss_position", rssSourceCode);
                DateEstimationResult result = new DateEstimationResult(positionDate, confidence, "rss_position",
                        String.format("Bloomberg RSS 순서 기반: %d/%d위", rssPosition + 1, totalArticles));
                
                if (result.isValid()) {
                    cacheService.saveDateExtractionResult(articleDto.getUrl(), "Bloomberg Economics", result);
                    return result;
                }
            }
        }
        
        // 4. 발행 패턴과 제목 키워드 기반 추정
        Optional<LocalDateTime> patternDate = estimateDateFromPublishingPattern(articleDto, rssSourceCode);
        if (patternDate.isPresent()) {
            double confidence = calculateConfidenceScore(patternDate.get(), "pattern", rssSourceCode);
            DateEstimationResult result = new DateEstimationResult(patternDate, confidence, "pattern",
                    "Bloomberg 발행 패턴 및 제목 분석 기반");
            
            if (result.isValid()) {
                cacheService.saveDateExtractionResult(articleDto.getUrl(), "Bloomberg Economics", result);
                return result;
            }
        }
        
        // 모든 방법 실패
        return new DateEstimationResult(Optional.empty(), 0.0, "failed", "모든 추정 방법 실패");
    }
    
    @Override
    public String getTypicalPublishingHours(String rssSourceCode) {
        return "0-23"; // Bloomberg는 24시간 발행
    }
    
    @Override
    public int getAverageArticlesPerDay(String rssSourceCode) {
        return 50; // Bloomberg는 하루 평균 50개 (매우 활발)
    }
    
    @Override
    public int getAssumedRecentArticleCount(String rssSourceCode) {
        return 8; // 상위 8개는 최신으로 간주 (빠른 업데이트)
    }
    
    @Override
    public boolean assumeRecentArticlesValid(String rssSourceCode) {
        return true; // Bloomberg는 순서가 매우 정확함
    }
    
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setSource("Bloomberg Economics");
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }
}