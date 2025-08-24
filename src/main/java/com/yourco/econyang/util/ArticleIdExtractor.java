package com.yourco.econyang.util;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RSS별 기사 고유 식별자 추출 유틸리티
 */
public class ArticleIdExtractor {
    
    // 매일경제: URL에서 숫자 ID 추출 (예: /economy/10987654, /english/11400481)
    private static final Pattern MAEIL_ECONOMY_PATTERN = Pattern.compile("/([0-9]{8,})(?:[/?]|$)");
    
    // KOTRA: dataIdx 쿼리 파라미터 (예: ?dataIdx=233281)
    private static final Pattern KOTRA_PATTERN = Pattern.compile("dataIdx=(\\d+)");
    
    // Investing.com: 마지막 하이픈 뒤 숫자 (예: israel-strikes-yemeni-capital-sanaa-4207948)
    private static final Pattern INVESTING_NEWS_PATTERN = Pattern.compile("([^/]+)-(\\d+)(?:/|\\?|#|$)");
    
    // Investing.com Analysis: analysis 경로에서 하이픈 뒤 숫자 (예: analysis/jackson-hole-200665775)
    private static final Pattern INVESTING_ANALYSIS_PATTERN = Pattern.compile("/analysis/[^/]+-([0-9]+)(?:/|\\?|#|$)");
    
    /**
     * RSS 소스별로 기사의 고유 식별자를 추출
     */
    public static String extractUniqueId(String sourceCode, String url) {
        if (url == null || url.trim().isEmpty()) {
            return generateHashId(url);
        }
        
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return generateHashId(url);
        }
        
        switch (sourceCode.toLowerCase()) {
            case "maeil_securities":
                return extractMaeilEconomyId(url);
                
            case "kotra_overseas":
                return extractKotraId(url);
                
            case "investing_news":
            case "investing_market":
            case "investing_commodities":
                return extractInvestingId(url);
                
            default:
                // 기본: URL 전체 해시값 사용
                return generateHashId(url);
        }
    }
    
    /**
     * 매일경제: URL 끝 숫자 ID 추출
     * 예: https://www.mk.co.kr/news/english/11400481 → maeil_11400481
     */
    private static String extractMaeilEconomyId(String url) {
        Matcher matcher = MAEIL_ECONOMY_PATTERN.matcher(url);
        
        if (matcher.find()) {
            String articleId = matcher.group(1);
            // 8자리 이상의 숫자만 유효한 기사 ID로 간주
            if (articleId.length() >= 8) {
                return "maeil_" + articleId;
            }
        }
        
        
        // 패턴에 맞지 않으면 해시 사용
        return generateHashId(url);
    }
    
    /**
     * KOTRA: dataIdx 파라미터 추출
     * 예: https://dream.kotra.or.kr/kotra/view.do?dataIdx=123456
     */
    private static String extractKotraId(String url) {
        Matcher matcher = KOTRA_PATTERN.matcher(url);
        
        if (matcher.find()) {
            return "kotra_" + matcher.group(1);
        }
        
        return generateHashId(url);
    }
    
    /**
     * Investing.com: 마지막 하이픈 뒤 숫자 ID 추출
     * 예1: https://www.investing.com/news/economy/israel-strikes-yemeni-capital-sanaa-4207948 → investing_4207948
     * 예2: https://www.investing.com/analysis/jackson-hole-1982-volcker-200665775 → investing_200665775
     */
    private static String extractInvestingId(String url) {
        // Analysis 경로 우선 체크 (더 구체적인 패턴)
        if (url.contains("/analysis/")) {
            Matcher analysisMatcher = INVESTING_ANALYSIS_PATTERN.matcher(url);
            if (analysisMatcher.find()) {
                return "investing_" + analysisMatcher.group(1);
            }
        }
        
        // 일반 뉴스 패턴 체크
        Matcher newsMatcher = INVESTING_NEWS_PATTERN.matcher(url);
        if (newsMatcher.find()) {
            String articleId = newsMatcher.group(2);
            // 숫자 ID가 충분히 길어야 유효한 기사 ID로 간주
            if (articleId.length() >= 6) {
                return "investing_" + articleId;
            }
        }
        
        return generateHashId(url);
    }
    
    /**
     * URL 전체의 SHA-256 해시값 생성 (기본 전략)
     */
    private static String generateHashId(String url) {
        if (url == null) {
            return "fallback_" + Math.abs("null".hashCode());
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "hash_" + hexString.toString().substring(0, 12);
            
        } catch (Exception e) {
            // 해시 생성 실패 시 URL 길이와 해시코드 조합 사용
            return "fallback_" + Math.abs(url.hashCode());
        }
    }
    
    /**
     * 고유 ID가 유효한지 검증
     */
    public static boolean isValidUniqueId(String uniqueId) {
        return uniqueId != null && 
               !uniqueId.trim().isEmpty() && 
               uniqueId.length() >= 5;
    }
    
    /**
     * 디버깅용: ID 추출 과정 로깅
     */
    public static void logIdExtraction(String sourceCode, String url, String extractedId) {
        System.out.println(String.format(
            "ID 추출 [%s]: %s -> %s", 
            sourceCode, 
            url.length() > 80 ? url.substring(0, 80) + "..." : url, 
            extractedId
        ));
    }
}