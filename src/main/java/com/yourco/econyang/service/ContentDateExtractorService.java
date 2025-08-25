package com.yourco.econyang.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 웹 콘텐츠에서 발행일자를 추출하는 서비스
 * 다양한 언론사의 날짜 형식을 지원
 */
@Service
public class ContentDateExtractorService {
    
    private final WebClient webClient;
    
    // 언론사별 날짜 선택자 매핑
    private static final Map<String, List<String>> SOURCE_DATE_SELECTORS = new HashMap<>();
    
    // 일반적인 날짜 메타태그 선택자들
    private static final List<String> COMMON_DATE_SELECTORS = Arrays.asList(
        "meta[property='article:published_time']",
        "meta[name='pubdate']", 
        "meta[name='publishdate']",
        "meta[name='date']",
        "meta[property='og:updated_time']",
        "time[datetime]",
        ".date", ".publish-date", ".published", ".timestamp",
        ".article-date", ".post-date", ".entry-date"
    );
    
    // 날짜 정규식 패턴들 (한국어/영어)
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
        // ISO 8601 형식
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2})"),
        // 한국어 형식: 2025년 8월 25일
        Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일"),
        // 한국어 형식: 2025.08.25
        Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})"),
        // 영어 형식: August 25, 2025
        Pattern.compile("([A-Za-z]+)\\s+(\\d{1,2}),\\s+(\\d{4})"),
        // 영어 형식: 25 Aug 2025
        Pattern.compile("(\\d{1,2})\\s+([A-Za-z]+)\\s+(\\d{4})"),
        // 슬래시 형식: 2025/08/25
        Pattern.compile("(\\d{4})/(\\d{1,2})/(\\d{1,2})")
    );
    
    // 월 이름 매핑 (영어 -> 숫자)
    private static final Map<String, Integer> MONTH_NAMES = new HashMap<>();
    
    static {
        // 언론사별 특화 선택자 설정
        SOURCE_DATE_SELECTORS.put("Financial Times", Arrays.asList(
            ".o-date", ".article__timestamp", "time[data-o-date-format]"
        ));
        
        SOURCE_DATE_SELECTORS.put("Bloomberg", Arrays.asList(
            "[data-module='ArticleTimestamp']", ".timestamp", "time"
        ));
        
        SOURCE_DATE_SELECTORS.put("MarketWatch", Arrays.asList(
            ".timestamp", ".article__timestamp", "time.timestamp"
        ));
        
        SOURCE_DATE_SELECTORS.put("매일경제", Arrays.asList(
            ".art_date", ".date", ".timestamp", "time"
        ));
        
        SOURCE_DATE_SELECTORS.put("Investing.com", Arrays.asList(
            ".articleHeader span", ".date", "time"
        ));
        
        // 월 이름 매핑 초기화
        MONTH_NAMES.put("january", 1); MONTH_NAMES.put("jan", 1);
        MONTH_NAMES.put("february", 2); MONTH_NAMES.put("feb", 2);
        MONTH_NAMES.put("march", 3); MONTH_NAMES.put("mar", 3);
        MONTH_NAMES.put("april", 4); MONTH_NAMES.put("apr", 4);
        MONTH_NAMES.put("may", 5);
        MONTH_NAMES.put("june", 6); MONTH_NAMES.put("jun", 6);
        MONTH_NAMES.put("july", 7); MONTH_NAMES.put("jul", 7);
        MONTH_NAMES.put("august", 8); MONTH_NAMES.put("aug", 8);
        MONTH_NAMES.put("september", 9); MONTH_NAMES.put("sep", 9); MONTH_NAMES.put("sept", 9);
        MONTH_NAMES.put("october", 10); MONTH_NAMES.put("oct", 10);
        MONTH_NAMES.put("november", 11); MONTH_NAMES.put("nov", 11);
        MONTH_NAMES.put("december", 12); MONTH_NAMES.put("dec", 12);
    }
    
    public ContentDateExtractorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * URL에서 HTML 콘텐츠를 가져와 발행일자를 추출
     */
    public Optional<LocalDateTime> extractPublishedDate(String url, String sourceName) {
        try {
            // HTML 콘텐츠 가져오기
            String htmlContent = fetchHtmlContent(url);
            if (htmlContent == null) {
                return Optional.empty();
            }
            
            Document doc = Jsoup.parse(htmlContent);
            
            // 1. 언론사별 특화 선택자로 시도
            Optional<LocalDateTime> dateFromSource = extractDateBySource(doc, sourceName);
            if (dateFromSource.isPresent()) {
                return dateFromSource;
            }
            
            // 2. 공통 메타태그에서 추출 시도
            Optional<LocalDateTime> dateFromMeta = extractDateFromMetaTags(doc);
            if (dateFromMeta.isPresent()) {
                return dateFromMeta;
            }
            
            // 3. 텍스트 콘텐츠에서 정규식으로 추출 시도
            return extractDateFromText(doc.text());
            
        } catch (Exception e) {
            System.err.println("날짜 추출 실패: " + url + " - " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * HTML 콘텐츠를 웹에서 가져오기
     */
    private String fetchHtmlContent(String url) {
        try {
            return webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            System.err.println("HTTP 요청 실패: " + url + " - " + e.getStatusCode());
            return null;
        } catch (Exception e) {
            System.err.println("콘텐츠 가져오기 실패: " + url + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 언론사별 특화 선택자로 날짜 추출
     */
    private Optional<LocalDateTime> extractDateBySource(Document doc, String sourceName) {
        List<String> selectors = SOURCE_DATE_SELECTORS.get(sourceName);
        if (selectors == null) {
            return Optional.empty();
        }
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                Optional<LocalDateTime> date = extractDateFromElement(element);
                if (date.isPresent()) {
                    return date;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 메타태그에서 날짜 추출
     */
    private Optional<LocalDateTime> extractDateFromMetaTags(Document doc) {
        for (String selector : COMMON_DATE_SELECTORS) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                Optional<LocalDateTime> date = extractDateFromElement(element);
                if (date.isPresent()) {
                    return date;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * HTML 요소에서 날짜 추출
     */
    private Optional<LocalDateTime> extractDateFromElement(Element element) {
        // datetime 속성 확인
        String datetime = element.attr("datetime");
        if (!datetime.isEmpty()) {
            Optional<LocalDateTime> date = parseDateTime(datetime);
            if (date.isPresent()) {
                return date;
            }
        }
        
        // content 속성 확인 (메타태그)
        String content = element.attr("content");
        if (!content.isEmpty()) {
            Optional<LocalDateTime> date = parseDateTime(content);
            if (date.isPresent()) {
                return date;
            }
        }
        
        // 요소의 텍스트에서 추출
        return extractDateFromText(element.text());
    }
    
    /**
     * 텍스트에서 정규식으로 날짜 추출
     */
    private Optional<LocalDateTime> extractDateFromText(String text) {
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                Optional<LocalDateTime> date = parseDateFromGroups(pattern, matcher);
                if (date.isPresent()) {
                    return date;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 정규식 그룹에서 날짜 파싱
     */
    private Optional<LocalDateTime> parseDateFromGroups(Pattern pattern, Matcher matcher) {
        try {
            String patternStr = pattern.pattern();
            
            if (patternStr.contains("년.*월.*일")) {
                // 한국어 형식: 2025년 8월 25일
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return Optional.of(LocalDateTime.of(year, month, day, 12, 0)); // 기본 시간 12:00
                
            } else if (patternStr.contains("\\\\.[^/]")) {
                // 점 형식: 2025.08.25
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
                
            } else if (patternStr.contains("[A-Za-z]+.*\\\\d{1,2}.*\\\\d{4}")) {
                // 영어 형식: August 25, 2025
                String monthName = matcher.group(1).toLowerCase();
                int day = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                Integer month = MONTH_NAMES.get(monthName);
                if (month != null) {
                    return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
                }
                
            } else if (patternStr.contains("\\\\d{1,2}.*[A-Za-z]+.*\\\\d{4}")) {
                // 영어 형식: 25 Aug 2025
                int day = Integer.parseInt(matcher.group(1));
                String monthName = matcher.group(2).toLowerCase();
                int year = Integer.parseInt(matcher.group(3));
                Integer month = MONTH_NAMES.get(monthName);
                if (month != null) {
                    return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
                }
                
            } else if (patternStr.contains("/")) {
                // 슬래시 형식: 2025/08/25
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return Optional.of(LocalDateTime.of(year, month, day, 12, 0));
            }
            
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + matcher.group(0) + " - " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * 다양한 날짜 형식 파싱
     */
    private Optional<LocalDateTime> parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // ISO 8601 형식 시도
        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"), // 타임존 포함
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
                    return Optional.of(ZonedDateTime.parse(dateStr, formatter).toLocalDateTime());
                } else if (formatter == DateTimeFormatter.ISO_INSTANT) {
                    return Optional.of(LocalDateTime.ofInstant(
                        java.time.Instant.parse(dateStr), 
                        java.time.ZoneId.systemDefault()
                    ));
                } else {
                    return Optional.of(LocalDateTime.parse(dateStr, formatter));
                }
            } catch (DateTimeParseException e) {
                // 다음 형식 시도
            }
        }
        
        // 정규식으로 텍스트에서 추출 시도
        return extractDateFromText(dateStr);
    }
}