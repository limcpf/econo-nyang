package com.yourco.econyang.strategy;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.ContentDateExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 발행일이 없는 RSS 소스용 시간 필터링 전략
 * - UniversalSmartStrategy와 통합하여 본문에서 날짜 추출 시도
 * - 추출 실패 시 제한적으로 수집
 * - Investing.com, KOTRA 등에 적용
 */
@Component
public class NoDateTimeFilterStrategy implements RssTimeFilterStrategy {
    
    @Value("${app.rss.noDateMaxArticles:10}")
    private int maxArticlesWithoutDate;
    
    @Autowired
    private UniversalSmartStrategy universalSmartStrategy;
    
    @Autowired
    private ContentDateExtractor contentDateExtractor;
    
    private static int articleCountWithoutDate = 0;
    
    @Override
    public boolean supports(String rssSourceCode) {
        return "investing_stock".equals(rssSourceCode) ||
               "investing_economic".equals(rssSourceCode) ||
               "investing_finance".equals(rssSourceCode) ||
               "investing_earnings".equals(rssSourceCode) ||
               "kotra_overseas".equals(rssSourceCode);
    }
    
    @Override
    public boolean shouldInclude(Article article, String rssSourceCode) {
        if (article.getPublishedAt() == null) {
            // 1. UniversalSmartStrategy로 날짜 추출 시도
            Optional<LocalDateTime> extractedDate = tryExtractDateWithSmartStrategy(article, rssSourceCode);
            
            if (extractedDate.isPresent()) {
                LocalDateTime dateTime = extractedDate.get();
                article.setPublishedAt(dateTime); // 추출된 날짜 설정
                
                boolean include = dateTime.isAfter(getCutoffDateTime(rssSourceCode));
                
                System.out.println(String.format(
                    "✅ 날짜 추출 성공 [%s]: %s (추출일: %s, 포함: %s)",
                    rssSourceCode,
                    article.getTitle(),
                    dateTime,
                    include ? "예" : "아니오"
                ));
                
                return include;
            }
            
            // 2. 날짜 추출 실패 시 제한된 개수만 허용 (기존 로직)
            if (articleCountWithoutDate < maxArticlesWithoutDate) {
                articleCountWithoutDate++;
                System.out.println(String.format(
                    "⚠️ 날짜 추출 실패, 제한적 허용 [%s]: %s (%d/%d)", 
                    rssSourceCode, 
                    article.getTitle(),
                    articleCountWithoutDate,
                    maxArticlesWithoutDate
                ));
                return true;
            } else {
                System.out.println(String.format(
                    "❌ 날짜 추출 실패, 제한 초과로 제외 [%s]: %s", 
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
     * UniversalSmartStrategy를 사용하여 날짜 추출 시도
     */
    private Optional<LocalDateTime> tryExtractDateWithSmartStrategy(Article article, String rssSourceCode) {
        try {
            // Article을 ArticleDto로 변환
            ArticleDto articleDto = convertToDto(article);
            
            // UniversalSmartStrategy로 날짜 추정 시도
            SmartDateFilterStrategy.DateEstimationResult result = 
                universalSmartStrategy.estimateDateWithFallbackChain(articleDto, 0, 1, rssSourceCode);
            
            if (result.isValid()) {
                return result.getEstimatedDate();
            }
            
        } catch (Exception e) {
            System.out.println(String.format(
                "날짜 추출 중 오류 [%s]: %s - %s", 
                rssSourceCode, 
                article.getTitle(), 
                e.getMessage()
            ));
        }
        
        return Optional.empty();
    }
    
    /**
     * Article을 ArticleDto로 변환
     */
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setSource(article.getSource());
        dto.setPublishedAt(article.getPublishedAt());
        return dto;
    }
    
    /**
     * 새로운 수집 사이클 시작시 카운터 리셋
     */
    public static void resetCounter() {
        articleCountWithoutDate = 0;
    }
}