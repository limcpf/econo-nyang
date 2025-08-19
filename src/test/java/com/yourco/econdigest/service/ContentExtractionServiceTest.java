package com.yourco.econdigest.service;

import com.yourco.econdigest.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
    void testExtractContent_유효하지않은기사() {
        // null 기사
        ArticleDto result1 = contentExtractionService.extractContent(null);
        assertNull(result1);
        
        // URL이 없는 기사
        ArticleDto noUrlArticle = new ArticleDto();
        noUrlArticle.setTitle("테스트 제목");
        noUrlArticle.setSource("test");
        
        ArticleDto result2 = contentExtractionService.extractContent(noUrlArticle);
        assertFalse(result2.isExtractSuccess());
        assertEquals("기사 URL이 없습니다", result2.getExtractError());
    }

    @Test
    void testExtractContent_잘못된URL() {
        // 존재하지 않는 URL
        ArticleDto article = new ArticleDto();
        article.setTitle("테스트 기사");
        article.setUrl("https://nonexistent-domain-12345.com/article");
        article.setSource("test");
        
        ArticleDto result = contentExtractionService.extractContent(article);
        
        assertFalse(result.isExtractSuccess());
        assertNotNull(result.getExtractError());
        assertNotNull(result.getExtractedAt());
        System.out.println("추출 실패 오류: " + result.getExtractError());
    }

    @Test 
    void testExtractContents_여러기사처리() {
        // 테스트용 기사 목록 생성 (실제 URL 대신 가상 URL 사용)
        ArticleDto article1 = new ArticleDto("test", "https://example.com/article1", "테스트 기사 1", null, null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "https://invalid-url-12345.com/article2", "테스트 기사 2", null, null, null, 1.0);
        ArticleDto article3 = new ArticleDto("test", "https://another-invalid-url-67890.com/article3", "테스트 기사 3", null, null, null, 1.0);
        
        List<ArticleDto> articles = Arrays.asList(article1, article2, article3);
        
        // 본문 추출 실행
        List<ArticleDto> results = contentExtractionService.extractContents(articles);
        
        // 결과 검증
        assertEquals(3, results.size());
        
        // 모든 기사에 대해 추출 시도가 되었는지 확인
        for (ArticleDto result : results) {
            assertNotNull(result.getExtractedAt());
            // 실제 웹사이트가 아니므로 대부분 실패할 것으로 예상
            if (!result.isExtractSuccess()) {
                assertNotNull(result.getExtractError());
            }
        }
        
        System.out.println("처리된 기사 수: " + results.size());
        for (int i = 0; i < results.size(); i++) {
            ArticleDto result = results.get(i);
            System.out.println((i + 1) + ". " + result.getTitle() + " - 성공: " + result.isExtractSuccess() + 
                             (result.getExtractError() != null ? " (오류: " + result.getExtractError() + ")" : ""));
        }
    }

    @Test
    void testCalculateSuccessRate() {
        // 성공/실패 기사 목록 생성
        ArticleDto successArticle = new ArticleDto();
        successArticle.setExtractSuccess(true);
        
        ArticleDto failedArticle1 = new ArticleDto();
        failedArticle1.setExtractSuccess(false);
        
        ArticleDto failedArticle2 = new ArticleDto();
        failedArticle2.setExtractSuccess(false);
        
        List<ArticleDto> articles = Arrays.asList(successArticle, failedArticle1, failedArticle2);
        
        // 성공률 계산
        double successRate = contentExtractionService.calculateSuccessRate(articles);
        
        // 1/3 = 0.333...
        assertEquals(1.0 / 3.0, successRate, 0.001);
        
        // 빈 목록 테스트
        double emptyRate = contentExtractionService.calculateSuccessRate(Arrays.asList());
        assertEquals(0.0, emptyRate);
        
        // null 테스트
        double nullRate = contentExtractionService.calculateSuccessRate(null);
        assertEquals(0.0, nullRate);
    }

    @Test
    void testGetSelectorsForSource_소스별선택자() {
        // ContentExtractionService의 private 메서드를 직접 테스트할 수 없으므로
        // 대신 다른 소스로 기사를 만들어서 선택자가 다르게 적용되는지 간접적으로 확인
        
        ArticleDto hankyungArticle = new ArticleDto();
        hankyungArticle.setSource("hankyung");
        hankyungArticle.setTitle("한국경제 테스트 기사");
        hankyungArticle.setUrl("https://invalid-hankyung-test.com");
        
        ArticleDto maeilArticle = new ArticleDto();
        maeilArticle.setSource("maeil");
        maeilArticle.setTitle("매일경제 테스트 기사");
        maeilArticle.setUrl("https://invalid-maeil-test.com");
        
        ArticleDto unknownArticle = new ArticleDto();
        unknownArticle.setSource("unknown");
        unknownArticle.setTitle("알수없는 소스 기사");
        unknownArticle.setUrl("https://invalid-unknown-test.com");
        
        // 추출 시도 (실패할 것으로 예상되지만 소스별 처리가 다른지 확인)
        ArticleDto result1 = contentExtractionService.extractContent(hankyungArticle);
        ArticleDto result2 = contentExtractionService.extractContent(maeilArticle);
        ArticleDto result3 = contentExtractionService.extractContent(unknownArticle);
        
        // 모두 실패할 것으로 예상되지만, 소스가 올바르게 유지되는지 확인
        assertEquals("hankyung", result1.getSource());
        assertEquals("maeil", result2.getSource());
        assertEquals("unknown", result3.getSource());
        
        assertFalse(result1.isExtractSuccess());
        assertFalse(result2.isExtractSuccess());
        assertFalse(result3.isExtractSuccess());
    }

    @Test
    void testContentExtraction_필드설정확인() {
        ArticleDto article = new ArticleDto();
        article.setTitle("테스트 기사");
        article.setUrl("https://invalid-test-url-12345.com/test");
        article.setSource("test");
        article.setPublishedAt(LocalDateTime.now());
        
        ArticleDto result = contentExtractionService.extractContent(article);
        
        // 기본 필드는 유지되어야 함
        assertEquals("테스트 기사", result.getTitle());
        assertEquals("https://invalid-test-url-12345.com/test", result.getUrl());
        assertEquals("test", result.getSource());
        assertNotNull(result.getPublishedAt());
        
        // 추출 관련 필드가 설정되었는지 확인
        assertNotNull(result.getExtractedAt());
        
        // 잘못된 URL이므로 실패할 것으로 예상
        assertFalse(result.isExtractSuccess());
        assertNotNull(result.getExtractError());
    }

    @Test
    void testArticleDto_새필드_기본값() {
        // 새로 추가된 필드들의 기본값 확인
        ArticleDto article = new ArticleDto();
        
        assertNull(article.getContent());
        assertNull(article.getExtractedAt());
        assertNull(article.getExtractError());
        assertFalse(article.isExtractSuccess()); // boolean의 기본값은 false
    }

    @Test
    void testArticleDto_새필드_setterGetter() {
        // 새로 추가된 필드들의 setter/getter 테스트
        ArticleDto article = new ArticleDto();
        LocalDateTime now = LocalDateTime.now();
        
        article.setContent("테스트 본문 내용");
        article.setExtractedAt(now);
        article.setExtractError("테스트 오류 메시지");
        article.setExtractSuccess(true);
        
        assertEquals("테스트 본문 내용", article.getContent());
        assertEquals(now, article.getExtractedAt());
        assertEquals("테스트 오류 메시지", article.getExtractError());
        assertTrue(article.isExtractSuccess());
    }
}