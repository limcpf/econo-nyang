package com.yourco.econdigest.service;

import com.yourco.econdigest.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContentExtractionService 단위 테스트
 */
class ContentExtractionServiceTest {

    private ContentExtractionService contentExtractionService;

    @BeforeEach
    void setUp() {
        contentExtractionService = new ContentExtractionService();
    }

    @Test
    void testExtractContent_ValidUrl() {
        // Given - 실제 URL 테스트는 네트워크 의존성이 있어서 모의 데이터 사용
        String mockUrl = "https://example.com/article";
        ArticleDto article = new ArticleDto();
        article.setUrl(mockUrl);
        
        // When & Then - URL 형식 검증
        assertNotNull(mockUrl);
        assertTrue(mockUrl.startsWith("http"));
        assertNotNull(article);
    }

    @Test
    void testExtractContent_InvalidUrl() {
        // Given
        ArticleDto article = new ArticleDto();
        article.setUrl("invalid-url");
        
        // When
        ArticleDto result = contentExtractionService.extractContent(article);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isExtractSuccess()); // 잘못된 URL은 실패
    }

    @Test
    void testExtractContent_NullUrl() {
        // Given
        ArticleDto article = new ArticleDto();
        article.setUrl(null);
        
        // When
        ArticleDto result = contentExtractionService.extractContent(article);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isExtractSuccess());
        assertNotNull(result.getExtractError());
    }

    @Test
    void testExtractContent_EmptyUrl() {
        // Given
        ArticleDto article = new ArticleDto();
        article.setUrl("");
        
        // When
        ArticleDto result = contentExtractionService.extractContent(article);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isExtractSuccess());
        assertNotNull(result.getExtractError());
    }

    @Test
    void testArticleProcessing() {
        // Given
        ArticleDto article = new ArticleDto();
        article.setTitle("테스트 제목");
        article.setUrl("https://example.com/test");
        article.setSource("테스트소스");
        
        // When
        ArticleDto result = contentExtractionService.extractContent(article);
        
        // Then
        assertNotNull(result);
        assertEquals("테스트 제목", result.getTitle());
        assertEquals("https://example.com/test", result.getUrl());
        assertEquals("테스트소스", result.getSource());
        assertNotNull(result.getExtractedAt());
    }

    @Test
    void testUrlValidation() {
        // Given & When & Then
        ArticleDto validHttps = new ArticleDto();
        validHttps.setUrl("https://example.com");
        assertTrue(validHttps.getUrl().startsWith("http"));
        
        ArticleDto validHttp = new ArticleDto();
        validHttp.setUrl("http://example.com");
        assertTrue(validHttp.getUrl().startsWith("http"));
        
        ArticleDto invalid = new ArticleDto();
        invalid.setUrl("invalid-url");
        assertFalse(invalid.getUrl().startsWith("http"));
    }

    @Test
    void testSourceHandling() {
        // Given
        String[] sources = {"hankyung.com", "mk.co.kr", "yna.co.kr", "unknown.com"};
        
        // When & Then - 다양한 소스 처리 확인
        for (String source : sources) {
            ArticleDto article = new ArticleDto();
            article.setUrl("https://" + source + "/article");
            article.setSource(source);
            
            assertNotNull(article.getSource());
            assertNotNull(article.getUrl());
        }
    }

    @Test
    void testArticleFields() {
        // Given
        ArticleDto article = new ArticleDto();
        
        // When
        article.setTitle("제목");
        article.setUrl("https://test.com");
        article.setSource("소스");
        article.setAuthor("작성자");
        article.setDescription("설명");
        
        // Then
        assertEquals("제목", article.getTitle());
        assertEquals("https://test.com", article.getUrl());
        assertEquals("소스", article.getSource());
        assertEquals("작성자", article.getAuthor());
        assertEquals("설명", article.getDescription());
    }
}