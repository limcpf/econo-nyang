package com.yourco.econyang.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL 경로에서 날짜 정보를 추출하는 유틸리티
 * 다양한 언론사의 URL 패턴 지원
 */
public class UrlDateExtractor {
    
    // URL 날짜 패턴들 (우선순위 순)
    private static final Pattern[] URL_DATE_PATTERNS = {
        // 1. 표준 형식: /2025/08/25/
        Pattern.compile(".*/(?<year>\\d{4})/(?<month>\\d{1,2})/(?<day>\\d{1,2})/.*"),
        
        // 2. 하이픈 구분: /2025-08-25/
        Pattern.compile(".*/(?<year>\\d{4})-(?<month>\\d{1,2})-(?<day>\\d{1,2})/.*"),
        
        // 3. 점 구분: /2025.08.25/  
        Pattern.compile(".*/(?<year>\\d{4})\\.(?<month>\\d{1,2})\\.(?<day>\\d{1,2})/.*"),
        
        // 4. 연속 숫자: /20250825/
        Pattern.compile(".*/(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})/.*"),
        
        // 5. 타임스탬프 포함: /2025/08/25/article-title-123456789/
        Pattern.compile(".*/(?<year>\\d{4})/(?<month>\\d{1,2})/(?<day>\\d{1,2})/[^/]*(?<timestamp>\\d{6,})/.*"),
        
        // 6. 쿼리 파라미터: ?date=2025-08-25 또는 ?published=20250825
        Pattern.compile(".*[?&](?:date|published|pubDate)=(?<year>\\d{4})[-.]?(?<month>\\d{1,2})[-.]?(?<day>\\d{1,2}).*"),
        
        // 7. Financial Times 패턴: /content/uuid-with-timestamp
        Pattern.compile(".*/content/[a-f0-9-]+-(?<timestamp>\\d{10}).*"),
        
        // 8. Bloomberg 패턴: /news/articles/2025-08-25/
        Pattern.compile(".*/articles/(?<year>\\d{4})-(?<month>\\d{1,2})-(?<day>\\d{1,2})/.*"),
        
        // 9. MarketWatch 패턴: /story/title-2025-08-25-uuid
        Pattern.compile(".*/story/.*-(?<year>\\d{4})-(?<month>\\d{1,2})-(?<day>\\d{1,2})-.*"),
        
        // 10. 매일경제 패턴: /news/articleView.html?idxno=202508250001
        Pattern.compile(".*idxno=(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})\\d+.*")
    };
    
    /**
     * URL에서 날짜 정보 추출
     */
    public static Optional<LocalDateTime> extractDateFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedUrl = url.toLowerCase().trim();
        
        for (Pattern pattern : URL_DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(normalizedUrl);
            if (matcher.matches()) {
                try {
                    return parseMatchedGroups(matcher, pattern);
                } catch (Exception e) {
                    // 현재 패턴 실패, 다음 패턴 시도
                    continue;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 정규식 매칭 그룹에서 날짜 파싱
     */
    private static Optional<LocalDateTime> parseMatchedGroups(Matcher matcher, Pattern pattern) {
        try {
            String patternStr = pattern.pattern();
            
            // 타임스탬프 패턴 처리
            if (patternStr.contains("timestamp")) {
                String timestampStr = matcher.group("timestamp");
                if (timestampStr != null) {
                    return parseTimestamp(timestampStr);
                }
            }
            
            // 년/월/일 패턴 처리
            String yearStr = null, monthStr = null, dayStr = null;
            
            try {
                yearStr = matcher.group("year");
                monthStr = matcher.group("month");  
                dayStr = matcher.group("day");
            } catch (IllegalArgumentException e) {
                // named group이 없는 경우 무시
                return Optional.empty();
            }
            
            if (yearStr != null && monthStr != null && dayStr != null) {
                int year = Integer.parseInt(yearStr);
                int month = Integer.parseInt(monthStr);
                int day = Integer.parseInt(dayStr);
                
                // 날짜 유효성 검사
                if (isValidDate(year, month, day)) {
                    return Optional.of(LocalDateTime.of(year, month, day, 12, 0)); // 기본 시간 12:00
                }
            }
            
        } catch (Exception e) {
            // 파싱 실패
        }
        
        return Optional.empty();
    }
    
    /**
     * Unix 타임스탬프 파싱
     */
    private static Optional<LocalDateTime> parseTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            
            // 10자리 Unix timestamp (초 단위)
            if (timestamp > 1000000000L && timestamp < 9999999999L) {
                return Optional.of(LocalDateTime.ofEpochSecond(timestamp, 0, 
                        java.time.ZoneOffset.systemDefault().getRules()
                            .getOffset(java.time.Instant.ofEpochSecond(timestamp))));
            }
            
            // 13자리 Unix timestamp (밀리초 단위)  
            if (timestamp > 1000000000000L && timestamp < 9999999999999L) {
                return Optional.of(LocalDateTime.ofEpochSecond(timestamp / 1000, 0,
                        java.time.ZoneOffset.systemDefault().getRules()
                            .getOffset(java.time.Instant.ofEpochMilli(timestamp))));
            }
            
        } catch (NumberFormatException e) {
            // 타임스탬프 파싱 실패
        }
        
        return Optional.empty();
    }
    
    /**
     * 날짜 유효성 검사
     */
    private static boolean isValidDate(int year, int month, int day) {
        if (year < 2000 || year > 2030) return false; // 합리적인 범위
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        
        try {
            LocalDateTime.of(year, month, day, 12, 0);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    /**
     * 특정 언론사에 특화된 URL 패턴 추출
     */
    public static Optional<LocalDateTime> extractDateFromUrlForSource(String url, String sourceName) {
        if (url == null || sourceName == null) {
            return Optional.empty();
        }
        
        // 언론사별 특화 처리
        switch (sourceName.toLowerCase()) {
            case "financial times":
                return extractFinancialTimesDate(url);
            case "bloomberg":
            case "bloomberg economics":
                return extractBloombergDate(url);
            case "marketwatch":
                return extractMarketWatchDate(url);
            case "매일경제":
                return extractMaeilDate(url);
            default:
                return extractDateFromUrl(url);
        }
    }
    
    /**
     * Financial Times URL 날짜 추출
     */
    private static Optional<LocalDateTime> extractFinancialTimesDate(String url) {
        // FT는 UUID 기반이므로 일반적인 방법 사용
        return extractDateFromUrl(url);
    }
    
    /**
     * Bloomberg URL 날짜 추출
     */
    private static Optional<LocalDateTime> extractBloombergDate(String url) {
        // Bloomberg 특화 패턴 먼저 시도
        Pattern bloombergPattern = Pattern.compile(".*/articles/(?<year>\\d{4})-(?<month>\\d{1,2})-(?<day>\\d{1,2})/.*");
        Matcher matcher = bloombergPattern.matcher(url.toLowerCase());
        
        if (matcher.matches()) {
            try {
                int year = Integer.parseInt(matcher.group("year"));
                int month = Integer.parseInt(matcher.group("month"));
                int day = Integer.parseInt(matcher.group("day"));
                
                if (isValidDate(year, month, day)) {
                    return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
                }
            } catch (Exception e) {
                // 실패시 일반 방법으로 폴백
            }
        }
        
        return extractDateFromUrl(url);
    }
    
    /**
     * MarketWatch URL 날짜 추출
     */
    private static Optional<LocalDateTime> extractMarketWatchDate(String url) {
        // MarketWatch는 종종 article ID에 날짜 포함
        return extractDateFromUrl(url);
    }
    
    /**
     * 매일경제 URL 날짜 추출
     */
    private static Optional<LocalDateTime> extractMaeilDate(String url) {
        // 매일경제 idxno 패턴: 202508250001 (YYYYMMDDXXXX)
        Pattern maeilPattern = Pattern.compile(".*idxno=(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})\\d+.*");
        Matcher matcher = maeilPattern.matcher(url.toLowerCase());
        
        if (matcher.matches()) {
            try {
                int year = Integer.parseInt(matcher.group("year"));
                int month = Integer.parseInt(matcher.group("month"));
                int day = Integer.parseInt(matcher.group("day"));
                
                if (isValidDate(year, month, day)) {
                    return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
                }
            } catch (Exception e) {
                // 실패시 일반 방법으로 폴백
            }
        }
        
        return extractDateFromUrl(url);
    }
}