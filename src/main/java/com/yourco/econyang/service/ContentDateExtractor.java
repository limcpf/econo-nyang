package com.yourco.econyang.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 본문에서 날짜 정보를 추출하는 서비스
 * RSS 메타데이터에서 발행일을 찾을 수 없는 경우 본문을 스캔하여 날짜를 추출
 */
@Service
public class ContentDateExtractor {
    
    // 다양한 날짜 패턴들
    private static final List<DatePattern> DATE_PATTERNS = new ArrayList<>();
    
    static {
        // 영어 패턴들
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(?:published|posted|updated|created)\\s*(?:on|at)?\\s*[:\\-]?\\s*(\\w+\\s+\\d{1,2},?\\s+\\d{4})", Pattern.CASE_INSENSITIVE),
            "MMM d, yyyy", "en"
        ));
        
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(?:published|posted|updated|created)\\s*(?:on|at)?\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", Pattern.CASE_INSENSITIVE),
            "M/d/yyyy", "en"
        ));
        
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(?:published|posted|updated|created)\\s*(?:on|at)?\\s*[:\\-]?\\s*(\\d{4}-\\d{1,2}-\\d{1,2})", Pattern.CASE_INSENSITIVE),
            "yyyy-M-d", "en"
        ));
        
        // 한국어 패턴들
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?:작성일|발행일|등록일|업데이트)\\s*[:\\-]?\\s*(\\d{4})[년.-](\\d{1,2})[월.-](\\d{1,2})[일]?", Pattern.CASE_INSENSITIVE),
            "yyyy년M월d일", "ko"
        ));
        
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?:작성일|발행일|등록일|업데이트)\\s*[:\\-]?\\s*(\\d{4})[.-](\\d{1,2})[.-](\\d{1,2})", Pattern.CASE_INSENSITIVE),
            "yyyy.M.d", "ko"
        ));
        
        // ISO 형식
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(?:published|updated|created)\\s*[:\\-]?\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE),
            "yyyy-MM-dd'T'HH:mm", "iso"
        ));
        
        // 상대 날짜 패턴들 (추후 확장 가능)
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(\\d+)\\s+hours?\\s+ago", Pattern.CASE_INSENSITIVE),
            "relative_hours", "relative"
        ));
        
        DATE_PATTERNS.add(new DatePattern(
            Pattern.compile("(?i)(\\d+)\\s+days?\\s+ago", Pattern.CASE_INSENSITIVE),
            "relative_days", "relative"
        ));
    }
    
    /**
     * HTML 본문에서 날짜를 추출
     */
    public Optional<LocalDateTime> extractDateFromContent(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // HTML 태그 제거 (간단한 정리)
        String cleanText = htmlContent.replaceAll("<[^>]+>", " ")
                                     .replaceAll("\\s+", " ")
                                     .trim();
        
        // 각 패턴을 시도
        for (DatePattern datePattern : DATE_PATTERNS) {
            Matcher matcher = datePattern.pattern.matcher(cleanText);
            
            while (matcher.find()) {
                try {
                    Optional<LocalDateTime> extracted = parseWithPattern(matcher, datePattern);
                    if (extracted.isPresent()) {
                        LocalDateTime dateTime = extracted.get();
                        
                        // 합리적인 날짜 범위 검증 (2020-2030년)
                        if (isReasonableDate(dateTime)) {
                            System.out.println("본문에서 날짜 추출 성공: " + dateTime + " (패턴: " + datePattern.type + ")");
                            return Optional.of(dateTime);
                        }
                    }
                } catch (Exception e) {
                    // 파싱 실패는 무시하고 다음 패턴 시도
                    continue;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 매칭된 패턴을 실제 날짜로 변환
     */
    private Optional<LocalDateTime> parseWithPattern(Matcher matcher, DatePattern datePattern) {
        try {
            if ("relative".equals(datePattern.type)) {
                return parseRelativeDate(matcher, datePattern.format);
            } else if ("ko".equals(datePattern.type)) {
                return parseKoreanDate(matcher);
            } else {
                String dateStr = matcher.group(1);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern.format);
                
                if (datePattern.format.contains("HH:mm")) {
                    return Optional.of(LocalDateTime.parse(dateStr, formatter));
                } else {
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return Optional.of(date.atStartOfDay());
                }
            }
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 상대 날짜 처리 ("2 hours ago", "3 days ago")
     */
    private Optional<LocalDateTime> parseRelativeDate(Matcher matcher, String format) {
        try {
            int amount = Integer.parseInt(matcher.group(1));
            LocalDateTime now = LocalDateTime.now();
            
            if ("relative_hours".equals(format)) {
                return Optional.of(now.minusHours(amount));
            } else if ("relative_days".equals(format)) {
                return Optional.of(now.minusDays(amount));
            }
        } catch (NumberFormatException e) {
            // 무시
        }
        
        return Optional.empty();
    }
    
    /**
     * 한국어 날짜 처리
     */
    private Optional<LocalDateTime> parseKoreanDate(Matcher matcher) {
        try {
            if (matcher.groupCount() >= 3) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                
                LocalDate date = LocalDate.of(year, month, day);
                return Optional.of(date.atStartOfDay());
            }
        } catch (Exception e) {
            // 무시
        }
        
        return Optional.empty();
    }
    
    /**
     * 합리적인 날짜 범위인지 검증
     */
    private boolean isReasonableDate(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime maxDate = now.plusDays(1); // 미래 1일까지 허용
        
        return dateTime.isAfter(minDate) && dateTime.isBefore(maxDate);
    }
    
    /**
     * 특정 언론사에 맞는 날짜 추출 시도
     */
    public Optional<LocalDateTime> extractDateForSource(String htmlContent, String sourceName) {
        // 기본 추출 시도
        Optional<LocalDateTime> basicResult = extractDateFromContent(htmlContent);
        if (basicResult.isPresent()) {
            return basicResult;
        }
        
        // 언론사별 특화 패턴 (필요시 확장)
        if (sourceName != null) {
            if (sourceName.toLowerCase().contains("investing")) {
                return extractInvestingComDate(htmlContent);
            } else if (sourceName.toLowerCase().contains("kotra")) {
                return extractKotraDate(htmlContent);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Investing.com 특화 날짜 추출
     */
    private Optional<LocalDateTime> extractInvestingComDate(String content) {
        // Investing.com 특화 패턴들
        Pattern investingPattern = Pattern.compile("(?i)data-date[=\"']([^\"']+)[\"']");
        Matcher matcher = investingPattern.matcher(content);
        
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                // Investing.com의 특정 날짜 형식 처리
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (isReasonableDate(dateTime)) {
                    return Optional.of(dateTime);
                }
            } catch (Exception e) {
                // 무시
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * KOTRA 특화 날짜 추출
     */
    private Optional<LocalDateTime> extractKotraDate(String content) {
        // KOTRA 특화 패턴들
        Pattern kotraPattern = Pattern.compile("(?i)작성일\\s*[:\\-]?\\s*(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = kotraPattern.matcher(content);
        
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                LocalDate date = LocalDate.parse(dateStr);
                if (date.isAfter(LocalDate.of(2020, 1, 1)) && 
                    date.isBefore(LocalDate.now().plusDays(1))) {
                    return Optional.of(date.atStartOfDay());
                }
            } catch (Exception e) {
                // 무시
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 날짜 패턴 정보를 담는 내부 클래스
     */
    private static class DatePattern {
        final Pattern pattern;
        final String format;
        final String type;
        
        DatePattern(Pattern pattern, String format, String type) {
            this.pattern = pattern;
            this.format = format;
            this.type = type;
        }
    }
}