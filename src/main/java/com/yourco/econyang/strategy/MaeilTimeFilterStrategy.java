package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 매일경제 전용 시간 필터링 전략
 * 매일경제는 RSS에서 가끔 오래된 날짜의 기사가 섞여있어서 더 엄격한 필터링 필요
 */
@Component
public class MaeilTimeFilterStrategy implements RssTimeFilterStrategy {
    
    @Value("${app.rss.maeilMaxAgeHours:48}")
    private int maeilMaxAgeHours;
    
    @Override
    public boolean supports(String rssSourceCode) {
        return "maeil_securities".equals(rssSourceCode);
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() == null) {
            System.out.println("매일경제 기사 발행일 없음, 제외: " + article.getUrl());
            return false; // 발행일이 없으면 제외 (품질 관리)
        }
        
        LocalDateTime cutoff = getCutoffDateTime(rssSourceCode);
        boolean include = article.getPublishedAt().isAfter(cutoff);
        
        if (!include) {
            System.out.println(String.format(
                "매일경제 기사 필터링됨: %s (발행: %s, 기준: %s)", 
                article.getTitle(),
                article.getPublishedAt(),
                cutoff
            ));
        } else {
            System.out.println(String.format(
                "매일경제 기사 포함됨: %s (발행: %s)", 
                article.getTitle(),
                article.getPublishedAt()
            ));
        }
        
        return include;
    }
    
    @Override
    public int getMaxAgeHours(String rssSourceCode) {
        return maeilMaxAgeHours;
    }
    
    @Override
    public String getStrategyName() {
        return "MaeilTimeFilter";
    }
}