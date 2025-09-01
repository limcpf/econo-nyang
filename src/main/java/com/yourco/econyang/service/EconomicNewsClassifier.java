package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.openai.service.OpenAiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 경제 뉴스 분류 및 필터링 서비스
 * 주식시장과 거시경제 중심으로 뉴스를 분류하고 품질을 평가합니다.
 */
@Service
public class EconomicNewsClassifier {

    @Autowired
    private OpenAiClient openAiClient;

    // 핵심 경제 키워드 (높은 우선순위)
    private static final Set<String> HIGH_PRIORITY_KEYWORDS = Set.of(
        // 주식시장
        "stock", "stocks", "equity", "shares", "market", "markets", "trading",
        "earnings", "revenue", "profit", "IPO", "dividend", "S&P 500", "Nasdaq", "Dow Jones",
        "주식", "증권", "주가", "코스피", "코스닥", "실적", "매출", "영업이익",
        
        // 거시경제 지표
        "GDP", "inflation", "CPI", "PPI", "unemployment", "employment", "interest rate",
        "federal reserve", "Fed", "central bank", "monetary policy", "recession", "growth",
        "경제성장률", "물가", "인플레이션", "금리", "한국은행", "중앙은행", "통화정책", "경기침체",
        
        // 금융시장
        "bond", "bonds", "treasury", "yield", "banking", "currency", "exchange rate",
        "채권", "국채", "수익률", "은행", "환율", "원달러"
    );

    // 허용되는 투자 자산 키워드 (중간 우선순위)
    private static final Set<String> ALLOWED_INVESTMENT_KEYWORDS = Set.of(
        "real estate", "REIT", "gold", "silver", "commodities", "oil", "crude", "copper",
        "부동산", "리츠", "금", "원자재", "유가", "국제유가", "구리"
    );

    // 기업 관련 키워드 (중간 우선순위)
    private static final Set<String> CORPORATE_KEYWORDS = Set.of(
        "merger", "acquisition", "M&A", "corporate", "earnings", "quarterly results", "guidance",
        "restructuring", "IPO", "listing", "buyback", "valuation", "shareholders", "corporation",
        "인수합병", "구조조정", "상장", "기업공개", "자사주", "기업"
    );

    // 강력 제외 키워드 (이 키워드가 포함되면 무조건 제외)
    private static final Set<String> STRONG_EXCLUDE_KEYWORDS = Set.of(
        // 엔터테인먼트
        "sports", "entertainment", "celebrity", "movie", "music", "drama", "gaming",
        "스포츠", "연예", "오락", "게임", "드라마", "영화", "음악", "아이돌",
        
        // 정치 (경제정책 제외)
        "election", "vote", "campaign", "president", "politics", "political",
        "선거", "투표", "정치", "대통령", "정당",
        
        // 건강/의료
        "health", "medical", "hospital", "doctor", "disease", "vaccine",
        "건강", "의료", "병원", "의사", "질병", "백신",
        
        // 사건사고
        "crime", "accident", "murder", "fire", "earthquake", "weather",
        "사건", "사고", "살인", "화재", "지진", "날씨"
    );

    /**
     * 기사의 경제 관련성과 품질을 평가합니다.
     */
    public NewsQualityScore evaluateNewsQuality(ArticleDto article) {
        String content = getFullContent(article).toLowerCase();
        
        // 강력 제외 키워드 체크 (우선순위 1)
        if (containsStrongExcludeKeywords(content)) {
            return new NewsQualityScore(0, "강력 제외 키워드 포함", NewsCategory.EXCLUDED);
        }
        
        // AI 기반 경제 관련성 분석 시도 (우선순위 2)
        try {
            NewsQualityScore aiScore = evaluateWithAI(article);
            if (aiScore != null) {
                return aiScore;
            }
        } catch (Exception e) {
            System.out.println("AI 경제 관련성 분석 실패, 키워드 기반으로 폴백: " + e.getMessage());
        }
        
        // 키워드 기반 폴백 분석 (우선순위 3)
        return evaluateWithKeywords(content);
    }
    
    /**
     * AI를 사용한 경제 관련성 분석
     */
    private NewsQualityScore evaluateWithAI(ArticleDto article) {
        String prompt = String.format(
            "다음 뉴스 기사의 경제/금융/투자 관련성을 0-10점으로 평가하고 이유를 설명해주세요.\n\n" +
            "제목: %s\n" +
            "내용: %s\n" +
            "출처: %s\n\n" +
            "평가 기준:\n" +
            "- 8-10점: 핵심 경제뉴스 (주식시장, 거시경제, 중앙은행 정책, GDP, 인플레이션 등)\n" +
            "- 5-7점: 일반 경제뉴스 (기업 실적, M&A, IPO, 투자 분석 등)\n" +
            "- 3-4점: 투자 관련 (부동산, REIT, 원자재, 투자 의견 등)\n" +
            "- 1-2점: 경제 관련성 낮음\n" +
            "- 0점: 비경제 뉴스 (연예, 스포츠, 정치, 사건사고 등)\n\n" +
            "응답 형식: 점수|이유\n" +
            "예시: 7|기업 SWOT 분석으로 투자자에게 유용한 경제 정보", 
            article.getTitle(),
            article.getDescription() != null ? article.getDescription() : "내용 없음",
            article.getSource()
        );
        
        String response = openAiClient.generateSimpleSummary(prompt, 100);
        return parseAIResponse(response);
    }
    
    /**
     * AI 응답 파싱
     */
    private NewsQualityScore parseAIResponse(String response) {
        try {
            if (response == null || !response.contains("|")) {
                return null;
            }
            
            String[] parts = response.split("\\|", 2);
            int score = Integer.parseInt(parts[0].trim());
            String reason = parts[1].trim();
            
            NewsCategory category = determineAICategory(score);
            
            return new NewsQualityScore(score, "AI분석: " + reason, category);
        } catch (Exception e) {
            System.out.println("AI 응답 파싱 실패: " + response);
            return null;
        }
    }
    
    /**
     * AI 점수 기반 카테고리 결정
     */
    private NewsCategory determineAICategory(int score) {
        if (score >= 8) {
            return NewsCategory.HIGH_PRIORITY;
        } else if (score >= 5) {
            return NewsCategory.MEDIUM_PRIORITY;  
        } else if (score >= 3) {
            return NewsCategory.ALLOWED_INVESTMENT;
        } else if (score >= 1) {
            return NewsCategory.LOW_PRIORITY;
        } else {
            return NewsCategory.EXCLUDED;
        }
    }
    
    /**
     * 키워드 기반 폴백 분석
     */
    private NewsQualityScore evaluateWithKeywords(String content) {
        // 핵심 경제 키워드 점수 (우선순위 2)
        int highPriorityScore = countKeywords(content, HIGH_PRIORITY_KEYWORDS) * 3;
        
        // 투자 자산 키워드 점수 (우선순위 3) 
        int investmentScore = countKeywords(content, ALLOWED_INVESTMENT_KEYWORDS) * 2;
        
        // 기업 키워드 점수 (우선순위 4)
        int corporateScore = countKeywords(content, CORPORATE_KEYWORDS) * 1;
        
        int totalScore = highPriorityScore + investmentScore + corporateScore;
        
        // 점수에 따른 분류
        NewsCategory category = determineCategory(totalScore, highPriorityScore, investmentScore);
        String reason = buildReason(highPriorityScore, investmentScore, corporateScore, totalScore);
        
        return new NewsQualityScore(totalScore, "키워드: " + reason, category);
    }

    /**
     * 뉴스 품질 점수가 임계값을 넘는지 확인
     */
    public boolean shouldIncludeNews(ArticleDto article, int minScore) {
        NewsQualityScore score = evaluateNewsQuality(article);
        boolean include = score.getScore() >= minScore && score.getCategory() != NewsCategory.EXCLUDED;
        
        // 로깅
        if (include) {
            System.out.println(String.format("✅ 경제뉴스 포함: [%s] %s - 점수: %d (%s)", 
                article.getSource(), truncate(article.getTitle(), 50), score.getScore(), score.getReason()));
        } else {
            System.out.println(String.format("❌ 경제뉴스 제외: [%s] %s - 점수: %d (%s)", 
                article.getSource(), truncate(article.getTitle(), 50), score.getScore(), score.getReason()));
        }
        
        return include;
    }

    private String getFullContent(ArticleDto article) {
        StringBuilder content = new StringBuilder();
        if (article.getTitle() != null) {
            content.append(article.getTitle()).append(" ");
        }
        if (article.getDescription() != null) {
            content.append(article.getDescription()).append(" ");
        }
        return content.toString();
    }

    private boolean containsStrongExcludeKeywords(String content) {
        return STRONG_EXCLUDE_KEYWORDS.stream()
                .anyMatch(keyword -> containsKeyword(content, keyword.toLowerCase()));
    }

    private int countKeywords(String content, Set<String> keywords) {
        return (int) keywords.stream()
                .mapToLong(keyword -> countKeywordOccurrences(content, keyword.toLowerCase()))
                .sum();
    }

    private boolean containsKeyword(String text, String keyword) {
        // 단어 경계를 고려한 키워드 매칭
        if (keyword.contains(" ")) {
            // 구문 키워드의 경우 직접 포함 여부 확인
            return text.contains(keyword);
        } else {
            // 한글의 경우 단어 경계가 달라서 단순 포함으로 체크
            if (keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
                return text.contains(keyword);
            } else {
                // 영어의 경우 단어 경계 확인
                String pattern = "\\b" + keyword.toLowerCase() + "\\b";
                return text.toLowerCase().matches(".*" + pattern + ".*");
            }
        }
    }

    private long countKeywordOccurrences(String text, String keyword) {
        if (!containsKeyword(text, keyword)) {
            return 0;
        }
        
        // 간단한 카운트 (더 정확한 구현 가능)
        if (keyword.contains(" ")) {
            return countOccurrences(text, keyword);
        } else if (keyword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            // 한글 키워드의 경우 단순 횟수 계산
            return countOccurrences(text, keyword);
        } else {
            // 영어의 경우 단어 경계 고려한 카운트
            String[] words = text.toLowerCase().split("\\W+");
            return java.util.Arrays.stream(words)
                    .mapToLong(word -> word.equals(keyword) ? 1 : 0)
                    .sum();
        }
    }

    private long countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private NewsCategory determineCategory(int totalScore, int highPriorityScore, int investmentScore) {
        if (totalScore >= 6) {
            return NewsCategory.HIGH_PRIORITY;
        } else if (totalScore >= 3 || highPriorityScore > 0) {
            return NewsCategory.MEDIUM_PRIORITY;  
        } else if (investmentScore > 0) {
            return NewsCategory.ALLOWED_INVESTMENT;
        } else {
            return NewsCategory.LOW_PRIORITY;
        }
    }

    private String buildReason(int highPriorityScore, int investmentScore, int corporateScore, int totalScore) {
        StringBuilder reason = new StringBuilder();
        if (highPriorityScore > 0) {
            reason.append("핵심경제:").append(highPriorityScore).append(" ");
        }
        if (investmentScore > 0) {
            reason.append("투자자산:").append(investmentScore).append(" ");
        }
        if (corporateScore > 0) {
            reason.append("기업:").append(corporateScore).append(" ");
        }
        if (reason.length() == 0) {
            reason.append("경제 관련성 낮음");
        } else {
            reason.append("(총점:").append(totalScore).append(")");
        }
        return reason.toString().trim();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 뉴스 품질 평가 결과
     */
    public static class NewsQualityScore {
        private final int score;
        private final String reason;
        private final NewsCategory category;

        public NewsQualityScore(int score, String reason, NewsCategory category) {
            this.score = score;
            this.reason = reason;
            this.category = category;
        }

        public int getScore() { return score; }
        public String getReason() { return reason; }
        public NewsCategory getCategory() { return category; }
    }

    /**
     * 뉴스 카테고리
     */
    public enum NewsCategory {
        HIGH_PRIORITY,      // 높은 우선순위 (핵심 경제 뉴스)
        MEDIUM_PRIORITY,    // 중간 우선순위 (일반 경제 뉴스)
        ALLOWED_INVESTMENT, // 허용된 투자 자산 뉴스
        LOW_PRIORITY,       // 낮은 우선순위
        EXCLUDED           // 제외됨
    }
}