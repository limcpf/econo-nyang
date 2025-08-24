package com.yourco.econyang.util;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RSS별 기사 고유 식별자 추출 유틸리티
 */
public class ArticleIdExtractor {
    
    private static final Pattern MAEIL_ECONOMY_PATTERN = Pattern.compile("/rss/(\\d+)/");
    private static final Pattern KOTRA_PATTERN = Pattern.compile("dataIdx=(\\d+)");
    private static final Pattern INVESTING_PATTERN = Pattern.compile("/(\\d+)$");
    
    /**
     * RSS 소스별로 기사의 고유 식별자를 추출
     */
    public static String extractUniqueId(String sourceCode, String url) {
        if (url == null || url.trim().isEmpty()) {
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
     * 매일경제: URL 패턴에서 숫자 ID 추출
     * 예: https://www.mk.co.kr/news/economy/10123456
     */
    private static String extractMaeilEconomyId(String url) {
        // URL 패턴에서 숫자 추출 시도
        Pattern pattern = Pattern.compile("/(\\d{8,})(?:[/?]|$)");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return "maeil_" + matcher.group(1);
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
     * Investing.com: URL 마지막 숫자 ID 추출
     * 예: https://www.investing.com/news/economy/some-news-123456
     */
    private static String extractInvestingId(String url) {
        // URL에서 숫자 ID 추출 (더 유연한 패턴)
        Pattern pattern = Pattern.compile("[-/](\\d{7,})(?:[/?#]|$)");
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return "investing_" + matcher.group(1);
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