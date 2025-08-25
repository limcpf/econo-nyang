package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 스마트 날짜 필터링 전략 인터페이스
 * 기존 RssTimeFilterStrategy를 확장하여 지능형 날짜 추정 기능 추가
 */
public interface SmartDateFilterStrategy extends RssTimeFilterStrategy {
    
    /**
     * URL 패턴에서 날짜 추출 시도
     * 
     * @param url 기사 URL
     * @param rssSourceCode RSS 소스 코드  
     * @return 추출된 날짜 (Optional)
     */
    Optional<LocalDateTime> extractDateFromUrl(String url, String rssSourceCode);
    
    /**
     * RSS 피드 순서를 기반으로 날짜 추정
     * 
     * @param rssPosition RSS 피드에서의 위치 (0부터 시작)
     * @param totalArticles 총 기사 수
     * @param rssSourceCode RSS 소스 코드
     * @return 추정된 날짜 (Optional)
     */
    Optional<LocalDateTime> estimateDateFromRssPosition(int rssPosition, int totalArticles, String rssSourceCode);
    
    /**
     * 언론사별 발행 패턴을 기반으로 날짜 추정
     * 
     * @param articleDto 기사 정보
     * @param rssSourceCode RSS 소스 코드
     * @return 추정된 날짜 (Optional)
     */
    Optional<LocalDateTime> estimateDateFromPublishingPattern(ArticleDto articleDto, String rssSourceCode);
    
    /**
     * 추정된 날짜의 신뢰도 점수 계산 (0.0 ~ 1.0)
     * 
     * @param estimatedDate 추정된 날짜
     * @param extractionMethod 추출 방법 ("url", "rss_position", "pattern", "cache")
     * @param rssSourceCode RSS 소스 코드
     * @return 신뢰도 점수
     */
    double calculateConfidenceScore(LocalDateTime estimatedDate, String extractionMethod, String rssSourceCode);
    
    /**
     * 폴백 체인을 사용하여 최적의 날짜 추정
     * 
     * @param articleDto 기사 정보
     * @param rssPosition RSS 피드에서의 위치
     * @param totalArticles 총 기사 수
     * @param rssSourceCode RSS 소스 코드
     * @return 추정 결과 (날짜, 신뢰도, 추출 방법 포함)
     */
    DateEstimationResult estimateDateWithFallbackChain(ArticleDto articleDto, int rssPosition, 
                                                      int totalArticles, String rssSourceCode);
    
    /**
     * 해당 언론사의 일반적인 발행 시간대
     * 
     * @param rssSourceCode RSS 소스 코드
     * @return 발행 시간대 (24시간 형식, 예: 9-18시)
     */
    default String getTypicalPublishingHours(String rssSourceCode) {
        return "6-23"; // 기본값: 6시~23시
    }
    
    /**
     * 해당 언론사의 평균 일일 기사 발행 수
     * 
     * @param rssSourceCode RSS 소스 코드  
     * @return 평균 일일 기사 수
     */
    default int getAverageArticlesPerDay(String rssSourceCode) {
        return 20; // 기본값: 하루 20개
    }
    
    /**
     * RSS 피드에서 최신 N개 기사는 24시간 이내로 간주할지 여부
     * 
     * @param rssSourceCode RSS 소스 코드
     * @return true면 최신 N개는 항상 포함
     */
    default boolean assumeRecentArticlesValid(String rssSourceCode) {
        return true;
    }
    
    /**
     * 최신 기사로 간주할 개수
     * 
     * @param rssSourceCode RSS 소스 코드
     * @return 최신 기사 개수
     */  
    default int getAssumedRecentArticleCount(String rssSourceCode) {
        return Math.min(5, getAverageArticlesPerDay(rssSourceCode) / 4); // 하루 발행량의 1/4 또는 5개
    }
    
    /**
     * 날짜 추정 결과를 담는 클래스
     */
    class DateEstimationResult {
        private final Optional<LocalDateTime> estimatedDate;
        private final double confidenceScore;
        private final String extractionMethod;
        private final String details;
        
        public DateEstimationResult(Optional<LocalDateTime> estimatedDate, double confidenceScore, 
                                  String extractionMethod, String details) {
            this.estimatedDate = estimatedDate;
            this.confidenceScore = confidenceScore;
            this.extractionMethod = extractionMethod;
            this.details = details;
        }
        
        public Optional<LocalDateTime> getEstimatedDate() { return estimatedDate; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getExtractionMethod() { return extractionMethod; }
        public String getDetails() { return details; }
        
        public boolean isValid() {
            return estimatedDate.isPresent() && confidenceScore > 0.3; // 신뢰도 30% 이상
        }
        
        @Override
        public String toString() {
            return String.format("DateEstimationResult{date=%s, confidence=%.2f, method=%s, details=%s}",
                    estimatedDate.orElse(null), confidenceScore, extractionMethod, details);
        }
    }
}