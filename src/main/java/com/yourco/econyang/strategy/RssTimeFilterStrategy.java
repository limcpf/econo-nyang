package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;

import java.time.LocalDateTime;

/**
 * RSS별 시간 필터링 전략 인터페이스
 */
public interface RssTimeFilterStrategy {
    
    /**
     * RSS 소스 코드에 해당하는지 확인
     */
    boolean supports(String rssSourceCode);
    
    /**
     * 기사가 수집 대상인지 시간 기준으로 판단
     * 
     * @param article RSS에서 파싱된 기사
     * @param rssSourceCode RSS 소스 코드
     * @return true면 수집 대상, false면 필터링 제외
     */
    boolean shouldInclude(Article article, String rssSourceCode);
    
    /**
     * 해당 RSS의 최대 허용 나이 (시간 단위)
     */
    int getMaxAgeHours(String rssSourceCode);
    
    /**
     * 기준 시간 계산 (현재 시각 기준)
     */
    default LocalDateTime getCutoffDateTime(String rssSourceCode) {
        return LocalDateTime.now().minusHours(getMaxAgeHours(rssSourceCode));
    }
    
    /**
     * 전략 이름 (로깅용)
     */
    String getStrategyName();
}