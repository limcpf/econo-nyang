package com.yourco.econdigest.service;

import com.yourco.econdigest.config.RssSourcesConfig;
import com.yourco.econdigest.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RssFeedService 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class RssFeedServiceTest {

    private RssFeedService rssFeedService;
    private RssSourcesConfig.RssSource testSource;

    @BeforeEach
    void setUp() {
        rssFeedService = new RssFeedService();
        
        // 테스트용 RSS 소스 생성
        testSource = new RssSourcesConfig.RssSource();
        testSource.setName("테스트 소스");
        testSource.setCode("test");
        testSource.setUrl("https://example.com/test.xml");
        testSource.setWeight(1.0);
        testSource.setEnabled(true);
        testSource.setTimeout(5000);
        testSource.setRetryCount(2);
    }

    @Test
    void testApplyFilters_포함키워드필터() {
        // 테스트 기사 생성
        ArticleDto article1 = new ArticleDto("test", "url1", "경제 성장률 상승", "경제가 좋아지고 있다", null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "url2", "스포츠 경기 결과", "축구 경기가 열렸다", null, null, 1.0);
        ArticleDto article3 = new ArticleDto("test", "url3", "금리 인상 발표", "중앙은행이 금리를 올렸다", null, null, 1.0);
        
        List<ArticleDto> articles = Arrays.asList(article1, article2, article3);
        
        // 필터 설정
        RssSourcesConfig.FilterConfig filters = new RssSourcesConfig.FilterConfig();
        filters.setIncludeKeywords(Arrays.asList("경제", "금리"));
        
        // 필터링 적용
        List<ArticleDto> filtered = rssFeedService.applyFilters(articles, filters);
        
        // 경제, 금리 관련 기사만 남아야 함
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(article1));
        assertFalse(filtered.contains(article2));
        assertTrue(filtered.contains(article3));
    }

    @Test
    void testApplyFilters_제외키워드필터() {
        // 테스트 기사 생성
        ArticleDto article1 = new ArticleDto("test", "url1", "경제 성장률 상승", "경제가 좋아지고 있다", null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "url2", "스포츠 경기 결과", "축구 경기가 열렸다", null, null, 1.0);
        ArticleDto article3 = new ArticleDto("test", "url3", "경제 스포츠 투자", "스포츠 관련 투자", null, null, 1.0);
        
        List<ArticleDto> articles = Arrays.asList(article1, article2, article3);
        
        // 필터 설정
        RssSourcesConfig.FilterConfig filters = new RssSourcesConfig.FilterConfig();
        filters.setExcludeKeywords(Arrays.asList("스포츠"));
        
        // 필터링 적용
        List<ArticleDto> filtered = rssFeedService.applyFilters(articles, filters);
        
        // 스포츠 관련 기사는 제외되어야 함
        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(article1));
        assertFalse(filtered.contains(article2));
        assertFalse(filtered.contains(article3)); // 경제 기사지만 스포츠 키워드 포함으로 제외
    }

    @Test
    void testApplyFilters_복합필터() {
        // 테스트 기사 생성
        ArticleDto article1 = new ArticleDto("test", "url1", "경제 성장률 상승", "경제가 좋아지고 있다", null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "url2", "스포츠 경기 결과", "축구 경기가 열렸다", null, null, 1.0);
        ArticleDto article3 = new ArticleDto("test", "url3", "금리 정책 발표", "중앙은행이 정책을 발표했다", null, null, 1.0);
        ArticleDto article4 = new ArticleDto("test", "url4", "연예인 경제활동", "연예인이 투자를 했다", null, null, 1.0);
        
        List<ArticleDto> articles = Arrays.asList(article1, article2, article3, article4);
        
        // 필터 설정 (경제 관련 포함, 스포츠/연예 제외)
        RssSourcesConfig.FilterConfig filters = new RssSourcesConfig.FilterConfig();
        filters.setIncludeKeywords(Arrays.asList("경제", "금리"));
        filters.setExcludeKeywords(Arrays.asList("스포츠", "연예"));
        
        // 필터링 적용
        List<ArticleDto> filtered = rssFeedService.applyFilters(articles, filters);
        
        // 경제/금리 관련이면서 스포츠/연예가 아닌 기사만 남아야 함
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(article1));
        assertFalse(filtered.contains(article2)); // 스포츠 포함
        assertTrue(filtered.contains(article3));
        assertFalse(filtered.contains(article4)); // 연예 포함으로 제외
    }

    @Test
    void testApplyFilters_필터없음() {
        // 테스트 기사 생성
        ArticleDto article1 = new ArticleDto("test", "url1", "경제 뉴스", "경제 내용", null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "url2", "스포츠 뉴스", "스포츠 내용", null, null, 1.0);
        
        List<ArticleDto> articles = Arrays.asList(article1, article2);
        
        // 필터 없음
        RssSourcesConfig.FilterConfig filters = new RssSourcesConfig.FilterConfig();
        
        // 필터링 적용
        List<ArticleDto> filtered = rssFeedService.applyFilters(articles, filters);
        
        // 모든 기사가 남아야 함
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(article1));
        assertTrue(filtered.contains(article2));
    }

    @Test
    void testFetchAllArticles_중복제거() {
        // 실제 RSS 피드 테스트는 외부 의존성이 있으므로
        // 여기서는 중복 제거 로직만 간접적으로 테스트할 수 있는 구조 확인
        
        // ArticleDto의 equals 메서드가 URL 기반으로 동작하는지 확인
        ArticleDto article1 = new ArticleDto("test", "http://example.com/1", "제목1", null, null, null, 1.0);
        ArticleDto article2 = new ArticleDto("test", "http://example.com/1", "제목2", null, null, null, 1.0); // 같은 URL
        ArticleDto article3 = new ArticleDto("test", "http://example.com/2", "제목3", null, null, null, 1.0);
        
        assertEquals(article1, article2); // URL이 같으면 같은 기사로 판단
        assertNotEquals(article1, article3); // URL이 다르면 다른 기사
    }

    @Test
    void testRssSourceConfig_기본값() {
        // RssSource의 기본값 확인
        RssSourcesConfig.RssSource source = new RssSourcesConfig.RssSource();
        
        assertEquals(1.0, source.getWeight());
        assertTrue(source.isEnabled());
        assertEquals(10000, source.getTimeout());
        assertEquals(3, source.getRetryCount());
    }

    @Test
    void testGlobalConfig_기본값() {
        // GlobalConfig의 기본값 확인
        RssSourcesConfig.GlobalConfig config = new RssSourcesConfig.GlobalConfig();
        
        assertEquals("EconDigest/1.0", config.getUserAgent());
        assertEquals(10000, config.getDefaultTimeout());
        assertEquals(3, config.getDefaultRetryCount());
        assertEquals(20, config.getMaxArticlesPerSource());
        assertEquals(3600, config.getMinPublishInterval());
    }

    @Test
    void testArticleDto_생성과필드() {
        // ArticleDto 생성 및 필드 테스트
        ArticleDto article = new ArticleDto("hankyung", "http://example.com", "테스트 제목", 
                                          "테스트 설명", "테스트 작성자", null, 0.9);
        
        assertEquals("hankyung", article.getSource());
        assertEquals("http://example.com", article.getUrl());
        assertEquals("테스트 제목", article.getTitle());
        assertEquals("테스트 설명", article.getDescription());
        assertEquals("테스트 작성자", article.getAuthor());
        assertEquals(0.9, article.getSourceWeight());
        assertNotNull(article.getFetchedAt()); // 생성 시 자동 설정
    }
}