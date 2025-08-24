package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 기본 RSS 시간 필터링 전략
 */
@Component
public class DefaultRssTimeFilterStrategy implements RssTimeFilterStrategy {
    
    @Value("${app.rss.defaultMaxAgeHours:24}")
    private int defaultMaxAgeHours;
    
    @Value("${app.rss.enableTimeFilter:true}")
    private boolean enableTimeFilter;
    
    @Override
    public boolean supports(String rssSourceCode) {
        // 다른 전용 전략이 없으면 기본 전략 사용
        return true;
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (!enableTimeFilter) {
            return true; // 시간 필터 비활성화시 모든 기사 포함
        }
        
        if (article.getPublishedAt() == null) {
            // 발행일이 없는 기사는 제외하거나 별도 처리
            System.out.println("기사 발행일이 없음, 제외: " + article.getUrl());
            return false; // 발행일이 없으면 제외
        }
        
        LocalDateTime cutoff = getCutoffDateTime(rssSourceCode);
        boolean include = article.getPublishedAt().isAfter(cutoff);
        
        if (!include) {
            System.out.println(String.format(
                "기사 필터링됨 [%s]: %s (발행: %s, 기준: %s)", 
                rssSourceCode, 
                article.getTitle(),
                article.getPublishedAt(),
                cutoff
            ));
        }
        
        return include;
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return defaultMaxAgeHours;
    }
    
    @Override
    public String getStrategyName() {
        return "DefaultRssTimeFilter";
    }
}