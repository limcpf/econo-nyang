package com.yourco.econyang.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UrlDateExtractorTest {

    @Test
    void should_extract_date_from_standard_url_pattern() {
        String url = "https://example.com/2025/08/25/article-title";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_extract_date_from_hyphen_pattern() {
        String url = "https://news.com/articles/2025-08-25/breaking-news";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_extract_date_from_continuous_numbers() {
        String url = "https://news.com/story/20250825/article-id";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_extract_date_from_query_parameter() {
        String url = "https://news.com/article?date=2025-08-25&id=123";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_extract_date_from_maeil_pattern() {
        String url = "https://stock.mk.co.kr/news/articleView.html?idxno=202508250001";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrlForSource(url, "매일경제");
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_extract_date_from_bloomberg_pattern() {
        String url = "https://www.bloomberg.com/news/articles/2025-08-25/market-update-story";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrlForSource(url, "Bloomberg");
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }

    @Test
    void should_return_empty_for_invalid_url() {
        String url = "https://example.com/no-date-here/article";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertFalse(result.isPresent());
    }

    @Test
    void should_return_empty_for_null_url() {
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(null);
        
        assertFalse(result.isPresent());
    }

    @Test
    void should_reject_invalid_dates() {
        String url = "https://example.com/2025/13/40/invalid-date"; // 13월 40일
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertFalse(result.isPresent());
    }

    @Test
    void should_reject_unrealistic_years() {
        String url = "https://example.com/1999/08/25/too-old"; // 너무 오래된 년도
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertFalse(result.isPresent());
    }

    @Test
    void should_handle_dot_separated_dates() {
        String url = "https://news.com/articles/2025.08.25/story";
        
        Optional<LocalDateTime> result = UrlDateExtractor.extractDateFromUrl(url);
        
        assertTrue(result.isPresent());
        assertEquals(2025, result.get().getYear());
        assertEquals(8, result.get().getMonthValue());
        assertEquals(25, result.get().getDayOfMonth());
    }
}