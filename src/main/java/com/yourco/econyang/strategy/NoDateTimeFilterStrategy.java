package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 발행일이 없는 RSS 소스용 시간 필터링 전략
 * - 발행일이 없는 기사도 허용하되 제한적으로 수집
 * - Investing.com, KOTRA 등에 적용
 */
@Component
public class NoDateTimeFilterStrategy implements RssTimeFilterStrategy {
    
    @Value("${app.rss.noDateMaxArticles:10}")
    private int maxArticlesWithoutDate;
    
    private static int articleCountWithoutDate = 0;
    
    @Override
    public boolean supports(String rssSourceCode) {
        return "investing_news".equals(rssSourceCode) ||
               "investing_market".equals(rssSourceCode) ||
               "investing_commodities".equals(rssSourceCode) ||
               "kotra_overseas".equals(rssSourceCode);
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() == null) {
            // 발행일이 없는 기사는 제한된 개수만 허용
            if (articleCountWithoutDate < maxArticlesWithoutDate) {
                articleCountWithoutDate++;
                System.out.println(String.format(
                    "발행일 없는 기사 허용 [%s]: %s (%d/%d)", 
                    rssSourceCode, 
                    article.getTitle(),
                    articleCountWithoutDate,
                    maxArticlesWithoutDate
                ));
                return true;
            } else {
                System.out.println(String.format(
                    "발행일 없는 기사 제한 초과로 제외 [%s]: %s", 
                    rssSourceCode, 
                    article.getTitle()
                ));
                return false;
            }
        }
        
        // 발행일이 있는 기사는 일반적인 시간 필터링 적용
        LocalDateTime cutoff = getCutoffDateTime(rssSourceCode);
        boolean include = article.getPublishedAt().isAfter(cutoff);
        
        if (!include) {
            System.out.println(String.format(
                "시간 필터링으로 제외 [%s]: %s (발행: %s, 기준: %s)", 
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
        return 72; // 3일
    }
    
    @Override
    public String getStrategyName() {
        return "NoDateTimeFilter";
    }
    
    /**
     * 새로운 수집 사이클 시작시 카운터 리셋
     */
    public static void resetCounter() {
        articleCountWithoutDate = 0;
    }
}