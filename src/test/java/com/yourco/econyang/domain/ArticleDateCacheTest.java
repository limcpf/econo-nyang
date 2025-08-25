package com.yourco.econyang.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleDateCache 엔티티 단위 테스트
 */
class ArticleDateCacheTest {

    @Test
    void should_create_article_date_cache_with_valid_fields() {
        // Given
        String urlHash = "abc123hash";
        String sourceName = "Financial Times";
        LocalDateTime extractedDate = LocalDateTime.now().minusHours(2);
        String extractionMethod = "url";
        Double confidenceScore = 0.85;
        String extractionDetails = "Pattern matched: yyyy/mm/dd";

        // When
        ArticleDateCache cache = new ArticleDateCache(urlHash, sourceName, extractedDate, 
                                                     extractionMethod, confidenceScore, extractionDetails);

        // Then
        assertNotNull(cache);
        assertEquals(urlHash, cache.getUrlHash());
        assertEquals(sourceName, cache.getSourceName());
        assertEquals(extractedDate, cache.getExtractedDate());
        assertEquals(extractionMethod, cache.getExtractionMethod());
        assertEquals(confidenceScore, cache.getConfidenceScore());
        assertEquals(extractionDetails, cache.getExtractionDetails());
        assertNotNull(cache.getCreatedAt());
        assertEquals(1, cache.getVerificationCount());
        assertTrue(cache.getIsValid());
    }

    @Test
    void should_increment_verification_count() {
        // Given
        ArticleDateCache cache = createTestCache();
        LocalDateTime beforeIncrement = LocalDateTime.now();

        // When
        cache.incrementVerification();

        // Then
        assertEquals(2, cache.getVerificationCount());
        assertNotNull(cache.getLastVerifiedAt());
        assertTrue(cache.getLastVerifiedAt().isAfter(beforeIncrement) || 
                   cache.getLastVerifiedAt().equals(beforeIncrement));
    }

    @Test
    void should_check_recently_created() {
        // Given
        ArticleDateCache recentCache = createTestCache();
        ArticleDateCache oldCache = createTestCache();
        oldCache.setCreatedAt(LocalDateTime.now().minusHours(25));

        // When & Then
        assertTrue(recentCache.isRecentlyCreated(24));
        assertFalse(oldCache.isRecentlyCreated(24));
    }

    @Test
    void should_check_high_confidence() {
        // Given
        ArticleDateCache highConfidenceCache = createTestCache();
        highConfidenceCache.setConfidenceScore(0.8);
        
        ArticleDateCache lowConfidenceCache = createTestCache();
        lowConfidenceCache.setConfidenceScore(0.5);

        // When & Then
        assertTrue(highConfidenceCache.isHighConfidence());
        assertFalse(lowConfidenceCache.isHighConfidence());
    }

    @Test
    void should_generate_meaningful_toString() {
        // Given
        ArticleDateCache cache = createTestCache();
        cache.setId(123L);

        // When
        String toString = cache.toString();

        // Then
        assertTrue(toString.contains("ArticleDateCache"));
        assertTrue(toString.contains("123"));
        assertTrue(toString.contains("Financial Times"));
        assertTrue(toString.contains("url"));
        assertTrue(toString.contains("0.85"));
    }

    private ArticleDateCache createTestCache() {
        return new ArticleDateCache(
                "test_hash", 
                "Financial Times", 
                LocalDateTime.now().minusHours(2),
                "url", 
                0.85, 
                "Test extraction details"
        );
    }
}