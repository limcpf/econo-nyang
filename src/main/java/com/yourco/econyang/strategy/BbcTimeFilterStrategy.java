package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.springframework.stereotype.Component;

/**
 * BBC Business 전용 시간 필터링 전략
 * - 심층 분석 기사가 많아 상대적으로 긴 시간 기준 적용
 */
@Component
public class BbcTimeFilterStrategy implements RssTimeFilterStrategy {
    
    private static final int BBC_BUSINESS_MAX_HOURS = 48; // 48시간
    
    @Override
    public boolean supports(String rssSourceCode) {
        return "bbc_business".equals(rssSourceCode);
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() == null) {
            System.out.println("BBC 기사 발행일 없음, 포함: " + article.getUrl());
            return true;
        }
        
        boolean include = article.getPublishedAt().isAfter(getCutoffDateTime(rssSourceCode));
        
        if (!include) {
            System.out.println(String.format(
                "BBC 기사 필터링됨 [%d시간 기준]: %s (발행: %s)", 
                getMaxAgeHours(rssSourceCode),
                article.getTitle(),
                article.getPublishedAt()
            ));
        }
        
        return include;
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return BBC_BUSINESS_MAX_HOURS;
    }
    
    @Override
    public String getStrategyName() {
        return "BbcTimeFilter";
    }
}