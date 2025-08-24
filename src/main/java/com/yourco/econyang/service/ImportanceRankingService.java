package com.yourco.econyang.service;

import com.yourco.econyang.domain.Summary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 고급 중요도 산정 및 랭킹 서비스
 * AI 점수, 키워드 가중치, 뉴스 소스 신뢰도, 시간 가중치를 종합적으로 고려
 */
@Service
public class ImportanceRankingService {
    
    @Value("${app.ranking.maxArticles:10}")
    private int maxArticles;
    
    @Value("${app.ranking.minImportanceScore:3.0}")
    private double minImportanceScore;
    
    @Value("${app.ranking.timeDecayHours:24}")
    private int timeDecayHours;
    
    // 뉴스 소스별 신뢰도 가중치
    private static final Map<String, Double> SOURCE_RELIABILITY = Map.of(
        // 해외 주요 언론사 (높은 신뢰도)
        "bbc_business", 1.3,
        "ft_companies", 1.4,
        "economist", 1.5,
        "bloomberg_economics", 1.4,
        "marketwatch", 1.2,
        
        // 금융 정보 사이트 (실시간성 중시)
        "investing_news", 1.1,
        "investing_market", 1.2,
        "investing_commodities", 1.0,
        
        // 국내 기관/정부 (높은 신뢰도)
        "kotra_overseas", 1.3,
        "maeil_securities", 1.1
    );
    
    // 경제 섹터별 키워드 가중치
    private static final Map<String, SectorWeight> SECTOR_KEYWORDS = Map.of(
        "monetary_policy", new SectorWeight("통화정책", 1.5, Arrays.asList("금리", "central bank", "federal reserve", "통화", "monetary")),
        "market_trend", new SectorWeight("시장동향", 1.3, Arrays.asList("market", "trading", "주식", "증시", "stock")),
        "economic_indicator", new SectorWeight("경제지표", 1.4, Arrays.asList("GDP", "inflation", "employment", "인플레이션", "고용")),
        "corporate_finance", new SectorWeight("기업재무", 1.2, Arrays.asList("earnings", "revenue", "profit", "기업", "실적")),
        "international_trade", new SectorWeight("국제무역", 1.3, Arrays.asList("trade", "export", "import", "수출", "수입", "무역")),
        "real_estate", new SectorWeight("부동산", 1.1, Arrays.asList("부동산", "real estate", "housing", "property")),
        "commodities", new SectorWeight("원자재", 1.2, Arrays.asList("commodities", "oil", "gold", "원자재", "석유")),
        "currency", new SectorWeight("환율", 1.3, Arrays.asList("currency", "exchange rate", "환율", "달러", "원화"))
    );
    
    /**
     * 종합 중요도 점수 계산 및 랭킹
     */
    public List<Summary> calculateImportanceRanking(List<Summary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return new ArrayList<>();
        }
        
        System.out.println("=== 고급 중요도 산정 알고리즘 시작 ===");
        System.out.println("대상 Summary 개수: " + summaries.size());
        
        // 1. 각 Summary의 종합 점수 계산
        List<SummaryWithScore> scoredSummaries = summaries.stream()
                .map(this::calculateCompositeScore)
                .filter(scored -> scored.compositeScore >= minImportanceScore)
                .collect(Collectors.toList());
        
        // 2. 점수별 정렬 (내림차순)
        scoredSummaries.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));
        
        // 3. 섹터 균형 적용
        List<SummaryWithScore> balancedSummaries = applySecterBalance(scoredSummaries);
        
        // 4. 최대 개수 제한
        if (balancedSummaries.size() > maxArticles) {
            balancedSummaries = balancedSummaries.subList(0, maxArticles);
        }
        
        // 5. 최종 Summary 리스트 반환
        List<Summary> rankedSummaries = balancedSummaries.stream()
                .map(scored -> scored.summary)
                .collect(Collectors.toList());
        
        // 디버깅 정보 출력
        printRankingDetails(balancedSummaries);
        
        System.out.println("고급 중요도 산정 완료: " + rankedSummaries.size() + "개 선별");
        return rankedSummaries;
    }
    
    /**
     * 개별 Summary의 종합 점수 계산
     */
    private SummaryWithScore calculateCompositeScore(Summary summary) {
        // 1. AI 점수 (기본 점수)
        double aiScore = summary.getScore() != null ? 
            summary.getScore().doubleValue() : 5.0;
        
        // 2. 뉴스 소스 신뢰도 가중치
        String sourceCode = summary.getArticle().getSource();
        double sourceWeight = SOURCE_RELIABILITY.getOrDefault(sourceCode, 1.0);
        
        // 3. 키워드 가중치 (섹터별)
        double keywordWeight = calculateKeywordWeight(summary);
        
        // 4. 시간 가중치 (최신성)
        double timeWeight = calculateTimeWeight(summary);
        
        // 5. 종합 점수 계산
        double compositeScore = aiScore * sourceWeight * keywordWeight * timeWeight;
        
        return new SummaryWithScore(summary, compositeScore, aiScore, sourceWeight, keywordWeight, timeWeight);
    }
    
    /**
     * 키워드 기반 가중치 계산
     */
    private double calculateKeywordWeight(Summary summary) {
        if (summary.getBulletsList() == null || summary.getBulletsList().isEmpty()) {
            return 1.0; // 기본값
        }
        
        List<String> keywords = summary.getBulletsList();
        String title = summary.getArticle().getTitle();
        String content = summary.getSummaryText() + " " + summary.getWhyItMatters();
        String allText = (title + " " + content + " " + String.join(" ", keywords)).toLowerCase();
        
        double maxWeight = 1.0;
        String matchedSector = "";
        
        // 각 섹터별로 키워드 매칭 검사
        for (Map.Entry<String, SectorWeight> entry : SECTOR_KEYWORDS.entrySet()) {
            SectorWeight sectorWeight = entry.getValue();
            int matchCount = 0;
            
            for (String keyword : sectorWeight.keywords) {
                if (allText.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            
            if (matchCount > 0) {
                double weight = sectorWeight.weight + (matchCount * 0.1); // 매칭 개수만큼 추가 가중치
                if (weight > maxWeight) {
                    maxWeight = weight;
                    matchedSector = sectorWeight.name;
                }
            }
        }
        
        return maxWeight;
    }
    
    /**
     * 시간 기반 가중치 계산 (최신성)
     */
    private double calculateTimeWeight(Summary summary) {
        LocalDateTime createdAt = summary.getCreatedAt();
        if (createdAt == null) {
            return 1.0; // 기본값
        }
        
        long hoursAgo = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        
        if (hoursAgo <= 1) {
            return 1.3; // 1시간 이내: 높은 가중치
        } else if (hoursAgo <= 6) {
            return 1.2; // 6시간 이내: 보통 높은 가중치
        } else if (hoursAgo <= 12) {
            return 1.1; // 12시간 이내: 약간 높은 가중치
        } else if (hoursAgo <= 24) {
            return 1.0; // 24시간 이내: 기본값
        } else {
            // 24시간 이후: 시간에 따라 감소
            double decayFactor = Math.max(0.5, 1.0 - ((hoursAgo - 24) / (double) timeDecayHours));
            return decayFactor;
        }
    }
    
    /**
     * 섹터 균형 적용 (다양성 확보)
     */
    private List<SummaryWithScore> applySecterBalance(List<SummaryWithScore> sortedSummaries) {
        Map<String, List<SummaryWithScore>> sectorGroups = new HashMap<>();
        List<SummaryWithScore> balancedList = new ArrayList<>();
        
        // 섹터별로 그룹화
        for (SummaryWithScore scored : sortedSummaries) {
            String sector = identifySector(scored.summary);
            sectorGroups.computeIfAbsent(sector, k -> new ArrayList<>()).add(scored);
        }
        
        // 각 섹터에서 최대 3개씩 선택 (다양성 확보)
        int maxPerSector = Math.max(2, maxArticles / Math.max(sectorGroups.size(), 1));
        
        for (Map.Entry<String, List<SummaryWithScore>> entry : sectorGroups.entrySet()) {
            List<SummaryWithScore> sectorSummaries = entry.getValue();
            int count = Math.min(maxPerSector, sectorSummaries.size());
            balancedList.addAll(sectorSummaries.subList(0, count));
        }
        
        // 종합 점수로 재정렬
        balancedList.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));
        
        return balancedList;
    }
    
    /**
     * Summary의 주요 섹터 식별
     */
    private String identifySector(Summary summary) {
        if (summary.getBulletsList() == null) {
            return "general";
        }
        
        String allText = (summary.getArticle().getTitle() + " " + 
                         summary.getSummaryText() + " " + 
                         String.join(" ", summary.getBulletsList())).toLowerCase();
        
        for (Map.Entry<String, SectorWeight> entry : SECTOR_KEYWORDS.entrySet()) {
            SectorWeight sectorWeight = entry.getValue();
            for (String keyword : sectorWeight.keywords) {
                if (allText.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        
        return "general";
    }
    
    /**
     * 랭킹 세부정보 출력
     */
    private void printRankingDetails(List<SummaryWithScore> scoredSummaries) {
        System.out.println("\n=== 중요도 산정 결과 ===");
        
        for (int i = 0; i < scoredSummaries.size(); i++) {
            SummaryWithScore scored = scoredSummaries.get(i);
            Summary summary = scored.summary;
            
            System.out.println(String.format(
                "%d. [%.2f] %s (소스: %s)",
                i + 1,
                scored.compositeScore,
                summary.getArticle().getTitle().length() > 50 ? 
                    summary.getArticle().getTitle().substring(0, 50) + "..." : 
                    summary.getArticle().getTitle(),
                summary.getArticle().getSource()
            ));
            
            System.out.println(String.format(
                "   └ AI:%.1f × 소스:%.1f × 키워드:%.1f × 시간:%.1f = %.2f",
                scored.aiScore, scored.sourceWeight, scored.keywordWeight, 
                scored.timeWeight, scored.compositeScore
            ));
        }
        
        // 섹터 분포 출력
        Map<String, Long> sectorDistribution = scoredSummaries.stream()
                .collect(Collectors.groupingBy(
                    scored -> identifySector(scored.summary),
                    Collectors.counting()));
        
        System.out.println("\n섹터 분포:");
        sectorDistribution.forEach((sector, count) -> 
            System.out.println("  " + sector + ": " + count + "개"));
    }
    
    // === 내부 클래스 ===
    
    /**
     * 점수가 계산된 Summary 래퍼 클래스
     */
    private static class SummaryWithScore {
        final Summary summary;
        final double compositeScore;
        final double aiScore;
        final double sourceWeight;
        final double keywordWeight;
        final double timeWeight;
        
        SummaryWithScore(Summary summary, double compositeScore, double aiScore, 
                        double sourceWeight, double keywordWeight, double timeWeight) {
            this.summary = summary;
            this.compositeScore = compositeScore;
            this.aiScore = aiScore;
            this.sourceWeight = sourceWeight;
            this.keywordWeight = keywordWeight;
            this.timeWeight = timeWeight;
        }
    }
    
    /**
     * 섹터별 가중치 정보
     */
    private static class SectorWeight {
        final String name;
        final double weight;
        final List<String> keywords;
        
        SectorWeight(String name, double weight, List<String> keywords) {
            this.name = name;
            this.weight = weight;
            this.keywords = keywords;
        }
    }
}