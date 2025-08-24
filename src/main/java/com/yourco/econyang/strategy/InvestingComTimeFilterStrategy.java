package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.springframework.stereotype.Component;

/**
 * Investing.com 전용 시간 필터링 전략
 * - 실시간 시장 정보가 중요하므로 더 짧은 시간 기준 적용
 */
@Component
public class InvestingComTimeFilterStrategy implements RssTimeFilterStrategy {
    
    // Investing.com RSS별 개별 설정
    private static final int INVESTING_NEWS_MAX_HOURS = 12;      // 뉴스: 12시간
    private static final int INVESTING_MARKET_MAX_HOURS = 6;     // 시장 현황: 6시간  
    private static final int INVESTING_COMMODITIES_MAX_HOURS = 8; // 원자재: 8시간
    
    @Override
    public boolean supports(String rssSourceCode) {
        return rssSourceCode != null && rssSourceCode.startsWith("investing_");
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() == null) {
            System.out.println("Investing.com 기사 발행일 없음, 포함: " + article.getUrl());
            return true;
        }
        
        boolean include = article.getPublishedAt().isAfter(getCutoffDateTime(rssSourceCode));
        
        if (!include) {
            System.out.println(String.format(
                "Investing.com 기사 필터링됨 [%s, %d시간 기준]: %s (발행: %s)", 
                rssSourceCode,
                getMaxAgeHours(rssSourceCode),
                article.getTitle(),
                article.getPublishedAt()
            ));
        }
        
        return include;
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        switch (rssSourceCode) {
            case "investing_news":
                return INVESTING_NEWS_MAX_HOURS;
            case "investing_market":
                return INVESTING_MARKET_MAX_HOURS;
            case "investing_commodities":
                return INVESTING_COMMODITIES_MAX_HOURS;
            default:
                return INVESTING_NEWS_MAX_HOURS; // 기본값
        }
    }
    
    @Override
    public String getStrategyName() {
        return "InvestingComTimeFilter";
    }
}