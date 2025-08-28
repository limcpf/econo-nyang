package com.yourco.econyang.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSS 시간 필터링 전략 팩토리
 */
@Component
public class RssTimeFilterStrategyFactory {
    
    @Autowired
    private List<RssTimeFilterStrategy> allStrategies;
    
    private final Map<String, RssTimeFilterStrategy> strategyCache = new ConcurrentHashMap<>();
    private RssTimeFilterStrategy defaultStrategy;
    
    @PostConstruct
    public void initializeStrategies() {
        System.out.println("=== RSS 시간 필터링 전략 초기화 ===");
        
        for (RssTimeFilterStrategy strategy : allStrategies) {
            System.out.println("전략 등록: " + strategy.getStrategyName());
            
            // DefaultRssTimeFilterStrategy를 기본 전략으로 설정
            if (strategy instanceof DefaultRssTimeFilterStrategy) {
                defaultStrategy = strategy;
            }
        }
        
        if (defaultStrategy == null) {
            throw new IllegalStateException("DefaultRssTimeFilterStrategy를 찾을 수 없습니다.");
        }
        
        System.out.println("기본 전략 설정: " + defaultStrategy.getStrategyName());
        System.out.println("총 " + allStrategies.size() + "개 전략 로드 완료");
    }
    
    /**
     * RSS 소스 코드에 맞는 전략 반환
     */
    public RssTimeFilterStrategy getStrategy(String rssSourceCode) {
        if (rssSourceCode == null) {
            return defaultStrategy;
        }
        
        // 캐시에서 먼저 확인
        RssTimeFilterStrategy cached = strategyCache.get(rssSourceCode);
        if (cached != null) {
            return cached;
        }
        
        // 전용 전략 찾기 (기본 전략 제외)
        for (RssTimeFilterStrategy strategy : allStrategies) {
            if (!(strategy instanceof DefaultRssTimeFilterStrategy) && 
                strategy.supports(rssSourceCode)) {
                
                System.out.println(String.format(
                    "RSS [%s]에 전용 전략 적용: %s", 
                    rssSourceCode, 
                    strategy.getStrategyName()
                ));
                
                strategyCache.put(rssSourceCode, strategy);
                return strategy;
            }
        }
        
        // 전용 전략이 없으면 기본 전략 사용
        System.out.println(String.format(
            "RSS [%s]에 기본 전략 적용: %s", 
            rssSourceCode, 
            defaultStrategy.getStrategyName()
        ));
        
        strategyCache.put(rssSourceCode, defaultStrategy);
        return defaultStrategy;
    }
    
    /**
     * 모든 RSS 소스의 시간 설정 출력 (디버깅용)
     */
    public void printAllTimeSettings() {
        System.out.println("=== RSS별 시간 필터 설정 ===");
        
        // 현재 설정된 RSS 소스들 (application.yml에서 가져와야 하지만 여기서는 하드코딩)
        String[] rssSources = {
            "bbc_business", "ft_companies", "marketwatch", "bloomberg_economics", "economist",
            "investing_stock", "investing_economic", "investing_finance", "investing_earnings", 
            "kotra_overseas", "maeil_securities"
        };
        
        for (String sourceCode : rssSources) {
            RssTimeFilterStrategy strategy = getStrategy(sourceCode);
            int maxHours = strategy.getMaxAgeHours(sourceCode);
            
            System.out.println(String.format(
                "- %s: %d시간 (%s)", 
                sourceCode, 
                maxHours, 
                strategy.getStrategyName()
            ));
        }
    }
}