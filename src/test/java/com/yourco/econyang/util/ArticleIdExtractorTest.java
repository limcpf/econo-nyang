package com.yourco.econyang.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleIdExtractor 단위 테스트
 */
class ArticleIdExtractorTest {
    
    @Test
    void should_extract_maeil_economy_id() {
        // given
        String sourceCode = "maeil_securities";
        String url1 = "https://www.mk.co.kr/news/economy/10123456";
        String url2 = "https://www.mk.co.kr/news/stock-market/10987654?from=main";
        String url3 = "https://www.mk.co.kr/news/realestate/10555777/";
        
        // when
        String id1 = ArticleIdExtractor.extractUniqueId(sourceCode, url1);
        String id2 = ArticleIdExtractor.extractUniqueId(sourceCode, url2);
        String id3 = ArticleIdExtractor.extractUniqueId(sourceCode, url3);
        
        // then
        assertEquals("maeil_10123456", id1);
        assertEquals("maeil_10987654", id2);
        assertEquals("maeil_10555777", id3);
    }
    
    @Test
    void should_extract_kotra_id() {
        // given
        String sourceCode = "kotra_overseas";
        String url1 = "https://dream.kotra.or.kr/kotra/view.do?dataIdx=123456";
        String url2 = "https://dream.kotra.or.kr/kotra/view.do?dataIdx=789012&pBbsGroup=1";
        
        // when
        String id1 = ArticleIdExtractor.extractUniqueId(sourceCode, url1);
        String id2 = ArticleIdExtractor.extractUniqueId(sourceCode, url2);
        
        // then
        assertEquals("kotra_123456", id1);
        assertEquals("kotra_789012", id2);
    }
    
    @Test
    void should_extract_investing_id() {
        // given - 실제 Investing.com URL 패턴에 맞춘 예시
        String url1 = "https://www.investing.com/news/economy/fed-cuts-rates-12345678";
        String url2 = "https://www.investing.com/news/stock-market/tesla-earnings-23456789/";
        String url3 = "https://www.investing.com/news/commodities/oil-prices-rise-34567890?ext=related";
        
        // when
        String id1 = ArticleIdExtractor.extractUniqueId("investing_news", url1);
        String id2 = ArticleIdExtractor.extractUniqueId("investing_market", url2);
        String id3 = ArticleIdExtractor.extractUniqueId("investing_commodities", url3);
        
        // then
        assertEquals("investing_12345678", id1);
        assertEquals("investing_23456789", id2);
        assertEquals("investing_34567890", id3);
    }
    
    @Test
    void should_generate_hash_for_unknown_source() {
        // given
        String sourceCode = "bbc_business";
        String url = "https://www.bbc.com/news/business-12345";
        
        // when
        String id = ArticleIdExtractor.extractUniqueId(sourceCode, url);
        
        // then
        assertTrue(id.startsWith("hash_"));
        assertTrue(id.length() > 10);
    }
    
    @Test
    void should_generate_hash_for_invalid_patterns() {
        // given - 패턴에 맞지 않는 URL들
        String maeilUrl = "https://www.mk.co.kr/news/economy/abc123";  // 숫자 길이 부족
        String kotraUrl = "https://dream.kotra.or.kr/kotra/view.do?wrongParam=123";  // 잘못된 파라미터
        String investingUrl = "https://www.investing.com/news/economy/no-id-url";  // ID 없음
        
        // when
        String maeilId = ArticleIdExtractor.extractUniqueId("maeil_securities", maeilUrl);
        String kotraId = ArticleIdExtractor.extractUniqueId("kotra_overseas", kotraUrl);
        String investingId = ArticleIdExtractor.extractUniqueId("investing_news", investingUrl);
        
        // then
        assertTrue(maeilId.startsWith("hash_"));
        assertTrue(kotraId.startsWith("hash_"));
        assertTrue(investingId.startsWith("hash_"));
    }
    
    @Test
    void should_handle_null_and_empty_urls() {
        // given
        String sourceCode = "test_source";
        
        // when
        String nullId = ArticleIdExtractor.extractUniqueId(sourceCode, null);
        String emptyId = ArticleIdExtractor.extractUniqueId(sourceCode, "");
        String blankId = ArticleIdExtractor.extractUniqueId(sourceCode, "   ");
        
        // then
        assertTrue(nullId.startsWith("fallback_"));
        assertTrue(emptyId.startsWith("hash_"));  // 빈 문자열은 해시로 처리됨
        assertTrue(blankId.startsWith("hash_"));   // 공백 문자열도 해시로 처리됨
    }
    
    @Test
    void should_generate_consistent_hash_for_same_url() {
        // given
        String sourceCode = "test_source";
        String url = "https://example.com/news/article";
        
        // when
        String id1 = ArticleIdExtractor.extractUniqueId(sourceCode, url);
        String id2 = ArticleIdExtractor.extractUniqueId(sourceCode, url);
        String id3 = ArticleIdExtractor.extractUniqueId("another_source", url);
        
        // then
        assertEquals(id1, id2);  // 같은 URL은 같은 해시
        assertEquals(id1, id3);  // 소스가 달라도 URL이 같으면 같은 해시
    }
    
    @Test
    void should_validate_unique_id_correctly() {
        // given
        String validId1 = "maeil_123456";
        String validId2 = "kotra_789012";
        String validId3 = "hash_abcdef123456";
        
        String invalidId1 = null;
        String invalidId2 = "";
        String invalidId3 = "   ";
        String invalidId4 = "ab";  // 너무 짧음
        
        // when & then
        assertTrue(ArticleIdExtractor.isValidUniqueId(validId1));
        assertTrue(ArticleIdExtractor.isValidUniqueId(validId2));
        assertTrue(ArticleIdExtractor.isValidUniqueId(validId3));
        
        assertFalse(ArticleIdExtractor.isValidUniqueId(invalidId1));
        assertFalse(ArticleIdExtractor.isValidUniqueId(invalidId2));
        assertFalse(ArticleIdExtractor.isValidUniqueId(invalidId3));
        assertFalse(ArticleIdExtractor.isValidUniqueId(invalidId4));
    }
    
    @Test
    void should_extract_different_ids_for_different_articles() {
        // given
        String sourceCode = "maeil_securities";
        String url1 = "https://www.mk.co.kr/news/economy/10111111";
        String url2 = "https://www.mk.co.kr/news/economy/10222222";
        
        // when
        String id1 = ArticleIdExtractor.extractUniqueId(sourceCode, url1);
        String id2 = ArticleIdExtractor.extractUniqueId(sourceCode, url2);
        
        // then
        assertNotEquals(id1, id2);
        assertEquals("maeil_10111111", id1);
        assertEquals("maeil_10222222", id2);
    }
}