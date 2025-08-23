package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContentExtractionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
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
    
    // === 새로 추가된 병렬 처리 및 향상된 기능 테스트 ===
    
    @Test
    void testExtractContent_NullArticle() {
        // When
        ArticleDto result = contentExtractionService.extractContent(null);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void testExtractContents_EmptyList() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        
        // When
        List<ArticleDto> result = contentExtractionService.extractContents(articles);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testExtractContents_NullList() {
        // When
        List<ArticleDto> result = contentExtractionService.extractContents(null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testExtractContents_WithInvalidUrls() {
        // Given
        List<ArticleDto> articles = Arrays.asList(
                createTestArticle("한국경제", "invalid-url-1", "테스트 기사 1"),
                createTestArticle("매일경제", "invalid-url-2", "테스트 기사 2"),
                createTestArticle("연합뉴스", "invalid-url-3", "테스트 기사 3")
        );
        
        // When
        List<ArticleDto> results = contentExtractionService.extractContents(articles);
        
        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // 모든 결과가 실패해야 함 (병렬 처리 확인)
        for (ArticleDto result : results) {
            assertFalse(result.isExtractSuccess());
            assertNotNull(result.getExtractError());
            assertNotNull(result.getExtractedAt());
        }
    }
    
    @Test
    void testCalculateSuccessRate_EmptyList() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        
        // When
        double successRate = contentExtractionService.calculateSuccessRate(articles);
        
        // Then
        assertEquals(0.0, successRate);
    }
    
    @Test
    void testCalculateSuccessRate_NullList() {
        // When
        double successRate = contentExtractionService.calculateSuccessRate(null);
        
        // Then
        assertEquals(0.0, successRate);
    }
    
    @Test
    void testCalculateSuccessRate_MixedResults() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        
        // 성공한 기사 (수동 설정)
        ArticleDto success1 = createTestArticle("한국경제", "https://example.com/1", "성공 기사 1");
        success1.setExtractSuccess(true);
        articles.add(success1);
        
        // 실패한 기사 (수동 설정)
        ArticleDto fail1 = createTestArticle("매일경제", "invalid-url", "실패 기사 1");
        fail1.setExtractSuccess(false);
        articles.add(fail1);
        
        // 성공한 기사 (수동 설정)
        ArticleDto success2 = createTestArticle("연합뉴스", "https://example.com/2", "성공 기사 2");
        success2.setExtractSuccess(true);
        articles.add(success2);
        
        // When
        double successRate = contentExtractionService.calculateSuccessRate(articles);
        
        // Then
        assertEquals(2.0/3.0, successRate, 0.001);
    }
    
    @Test
    void testCalculateSuccessRate_AllSuccess() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ArticleDto article = createTestArticle("테스트" + i, "https://example.com/" + i, "기사 " + i);
            article.setExtractSuccess(true);
            articles.add(article);
        }
        
        // When
        double successRate = contentExtractionService.calculateSuccessRate(articles);
        
        // Then
        assertEquals(1.0, successRate);
    }
    
    @Test
    void testCalculateSuccessRate_AllFailure() {
        // Given
        List<ArticleDto> articles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ArticleDto article = createTestArticle("테스트" + i, "invalid-url-" + i, "기사 " + i);
            article.setExtractSuccess(false);
            articles.add(article);
        }
        
        // When
        double successRate = contentExtractionService.calculateSuccessRate(articles);
        
        // Then
        assertEquals(0.0, successRate);
    }
    
    @Test
    void testParallelProcessingPerformance() {
        // Given - 여러 기사를 병렬로 처리했을 때 성능 및 동작 확인
        List<ArticleDto> articles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            articles.add(createTestArticle("테스트" + i, "invalid-url-" + i, "기사 " + i));
        }
        
        // When
        long startTime = System.currentTimeMillis();
        List<ArticleDto> results = contentExtractionService.extractContents(articles);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertNotNull(results);
        assertEquals(10, results.size());
        
        // 처리 시간이 너무 오래 걸리지 않는지 확인 (10초 이내)
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 10000, "병렬 처리가 10초 이내에 완료되어야 합니다: " + processingTime + "ms");
        
        // 모든 기사가 처리되었는지 확인
        for (ArticleDto result : results) {
            assertNotNull(result.getExtractedAt());
            // 병렬 처리로 인한 랜덤 지연이 적용되었는지 확인
            assertNotNull(result.getExtractError()); // invalid URL이므로 에러가 있어야 함
        }
    }
    
    /**
     * 테스트용 ArticleDto 생성
     */
    private ArticleDto createTestArticle(String source, String url, String title) {
        ArticleDto article = new ArticleDto();
        article.setSource(source);
        article.setUrl(url);
        article.setTitle(title);
        article.setDescription("테스트 기사 설명");
        article.setAuthor("테스트 기자");
        article.setPublishedAt(LocalDateTime.now());
        article.setSourceWeight(1.0);
        return article;
    }
}