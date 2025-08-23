package com.yourco.econyang.service;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.repository.SummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * SummaryService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final String DEFAULT_MODEL = "gpt-4o";

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private ApiUsageMonitoringService usageMonitoringService;

    @InjectMocks
    private SummaryService summaryService;

    private Article testArticle;
    private EconomicSummaryResponse testAiResponse;

    @BeforeEach
    void setUp() {
        // 테스트 Article 생성 (본문을 충분히 길게 만들어 minContentLength=100을 넘기도록)
        testArticle = new Article("테스트소스", "https://example.com/test", "테스트 경제 뉴스");
        testArticle.setContent("이것은 테스트용 경제 뉴스 본문입니다. 경제 성장률이 상승하고 있으며 투자자들의 관심이 높아지고 있습니다. 시장 전망은 긍정적입니다. 주요 경제 지표들이 개선되고 있으며, 글로벌 시장에서도 한국 경제에 대한 신뢰가 높아지고 있는 상황입니다. 투자자들은 향후 성장 가능성에 주목하고 있으며, 정책당국도 긍정적인 전망을 내놓고 있습니다.");
        testArticle.setAuthor("테스트 기자");
        testArticle.setPublishedAt(LocalDateTime.now());

        // 테스트 AI 응답 생성
        testAiResponse = new EconomicSummaryResponse();
        testAiResponse.setSummary("경제 성장률 상승으로 시장 전망이 긍정적입니다.");
        testAiResponse.setAnalysis("투자자들의 관심 증가와 함께 경제 지표가 개선되고 있습니다.");
        testAiResponse.setImportanceScore(7);
        testAiResponse.setMarketImpact("상승");
        testAiResponse.setInvestorInterest("높음");
        testAiResponse.setConfidenceScore(8);
        testAiResponse.setKeywords(Arrays.asList("경제성장", "투자", "시장전망"));
        testAiResponse.setEconomicSectors(Arrays.asList("금융", "증권"));
        testAiResponse.setContext("경제 회복 신호가 뚜렷해지고 있음");

        // SummaryService의 필드 값 설정 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(summaryService, "defaultModel", DEFAULT_MODEL);
        ReflectionTestUtils.setField(summaryService, "aiSummaryEnabled", true);
        ReflectionTestUtils.setField(summaryService, "minContentLength", 100);
        ReflectionTestUtils.setField(summaryService, "maxRetries", 2);
    }

    @Test
    void testGenerateSummary_Success() {
        // Given
        lenient().when(summaryRepository.findByArticleAndModel(testArticle, DEFAULT_MODEL)).thenReturn(Optional.empty());
        lenient().when(openAiClient.isApiAvailable()).thenReturn(true);
        lenient().when(openAiClient.estimateTokens(anyString())).thenReturn(100);
        lenient().when(openAiClient.generateEconomicSummary(anyString(), anyString())).thenReturn(testAiResponse);
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Summary result = summaryService.generateSummary(testArticle, DEFAULT_MODEL, true);

        // Then
        assertNotNull(result);
        assertEquals("경제 성장률 상승으로 시장 전망이 긍정적입니다.", result.getSummaryText());
        assertEquals("투자자들의 관심 증가와 함께 경제 지표가 개선되고 있습니다.", result.getWhyItMatters());
        assertEquals(BigDecimal.valueOf(7), result.getScore());
        assertEquals(testArticle, result.getArticle());

        // 키워드가 bullets로 저장되었는지 확인
        List<String> bullets = result.getBulletsList();
        assertEquals(3, bullets.size());
        assertTrue(bullets.contains("경제성장"));
        assertTrue(bullets.contains("투자"));
        assertTrue(bullets.contains("시장전망"));

        // Glossary가 올바르게 설정되었는지 확인
        List<java.util.Map<String, String>> glossary = result.getGlossary();
        assertFalse(glossary.isEmpty());

        // Mock 검증
        verify(openAiClient).generateEconomicSummary(testArticle.getTitle(), testArticle.getContent());
        verify(summaryRepository).save(any(Summary.class));
        verify(usageMonitoringService).recordRequest(eq(DEFAULT_MODEL), eq(100), eq(300), eq(true), eq(null));
    }

    @Test
    void testGenerateSummary_ExistingSummary() {
        // Given
        Summary existingSummary = new Summary(testArticle, DEFAULT_MODEL, "기존 요약", "기존 분석");
        when(summaryRepository.findByArticleAndModel(testArticle, DEFAULT_MODEL)).thenReturn(Optional.of(existingSummary));

        // When
        Summary result = summaryService.generateSummary(testArticle, DEFAULT_MODEL, true);

        // Then
        assertSame(existingSummary, result);
        assertEquals("기존 요약", result.getSummaryText());

        // OpenAI API가 호출되지 않았는지 확인
        verify(openAiClient, never()).generateEconomicSummary(anyString(), anyString());
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    @Test
    void testGenerateSummary_ApiFailure() {
        // Given
        lenient().when(summaryRepository.findByArticleAndModel(testArticle, DEFAULT_MODEL)).thenReturn(Optional.empty());
        lenient().when(openAiClient.isApiAvailable()).thenReturn(true);
        lenient().when(openAiClient.estimateTokens(anyString())).thenReturn(100);
        lenient().when(openAiClient.generateEconomicSummary(anyString(), anyString()))
                .thenThrow(new RuntimeException("API 호출 실패"));
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Summary result = summaryService.generateSummary(testArticle, DEFAULT_MODEL, true);

        // Then
        assertNotNull(result);
        assertTrue(result.getSummaryText().contains("테스트 경제 뉴스")); // 폴백 요약 확인
        assertEquals(BigDecimal.valueOf(3), result.getScore()); // 낮은 점수

        // 실패가 기록되었는지 확인 (최대 3번 재시도 발생)
        verify(usageMonitoringService, atLeast(1)).recordRequest(eq(DEFAULT_MODEL), eq(0), eq(0), eq(false), anyString());
    }

    @Test
    void testGenerateSummary_ShortContent() {
        // Given
        testArticle.setContent("짧은 본문"); // 100자 미만
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Summary result = summaryService.generateSummary(testArticle, DEFAULT_MODEL, true);

        // Then
        assertNotNull(result);
        assertTrue(result.getSummaryText().contains("테스트 경제 뉴스")); // 폴백 요약
        assertEquals(BigDecimal.valueOf(3), result.getScore());

        // OpenAI API가 호출되지 않았는지 확인
        verify(openAiClient, never()).generateEconomicSummary(anyString(), anyString());
    }

    @Test
    void testGenerateSummary_NullContent() {
        // Given
        testArticle.setContent(null);
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Summary result = summaryService.generateSummary(testArticle, DEFAULT_MODEL, true);

        // Then
        assertNotNull(result);
        assertTrue(result.getWhyItMatters().contains("본문이 너무 짧거나 없습니다"));
        assertEquals(BigDecimal.valueOf(3), result.getScore());
    }

    @Test
    void testGenerateSummaries_BatchProcessing() {
        // Given
        Article article2 = new Article("소스2", "https://example.com/test2", "두 번째 뉴스");
        article2.setContent("두 번째 테스트 본문입니다. 이것도 충분히 긴 본문입니다. 경제 동향에 대한 내용을 포함하고 있습니다. 금리 상승에 따른 시장 상황 변화에 대한 자세한 분석과 전망도 포함되어 있습니다. 투자자들이 주목할 만한 중요한 정보를 담고 있어 충분한 길이의 본문입니다.");

        List<Article> articles = Arrays.asList(testArticle, article2);

        lenient().when(summaryRepository.findByArticleAndModel(any(Article.class), eq(DEFAULT_MODEL)))
                .thenReturn(Optional.empty());
        lenient().when(openAiClient.isApiAvailable()).thenReturn(true);
        lenient().when(openAiClient.estimateTokens(anyString())).thenReturn(100);
        lenient().when(openAiClient.generateEconomicSummary(anyString(), anyString()))
                .thenReturn(testAiResponse);
        lenient().when(summaryRepository.save(any(Summary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Summary> results = summaryService.generateSummaries(articles, DEFAULT_MODEL, true);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        for (Summary summary : results) {
            assertNotNull(summary);
            assertEquals("경제 성장률 상승으로 시장 전망이 긍정적입니다.", summary.getSummaryText());
            assertEquals(BigDecimal.valueOf(7), summary.getScore());
        }

        // 배치 처리 확인
        verify(openAiClient, times(2)).generateEconomicSummary(anyString(), anyString());
        verify(summaryRepository, times(2)).save(any(Summary.class));
    }

    @Test
    void testGenerateSummary_NullArticle() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            summaryService.generateSummary(null, DEFAULT_MODEL, true);
        });
    }

    @Test
    void testGenerateSummaries_EmptyList() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            summaryService.generateSummaries(Arrays.asList(), DEFAULT_MODEL, true);
        });
    }

    @Test
    void testIsAiSummaryAvailable() {
        // Given
        when(openAiClient.isApiAvailable()).thenReturn(true);
        
        // When & Then
        assertTrue(summaryService.isAiSummaryAvailable());

        // Given - API 불가능
        when(openAiClient.isApiAvailable()).thenReturn(false);

        // When & Then
        assertFalse(summaryService.isAiSummaryAvailable());
    }

    @Test
    void testGetBestSummaryForArticle() {
        // Given
        Summary bestSummary = new Summary(testArticle, DEFAULT_MODEL, "최고 요약", "최고 분석");
        bestSummary.setScore(BigDecimal.valueOf(9));

        when(summaryRepository.findBestByArticle(testArticle))
                .thenReturn(Arrays.asList(bestSummary));

        // When
        Optional<Summary> result = summaryService.getBestSummaryForArticle(testArticle);

        // Then
        assertTrue(result.isPresent());
        assertEquals(bestSummary, result.get());
        assertEquals("최고 요약", result.get().getSummaryText());
    }

    @Test
    void testGetSummariesByScoreRange() {
        // Given
        Summary highScoreSummary = new Summary(testArticle, DEFAULT_MODEL, "고점수 요약", "분석");
        highScoreSummary.setScore(BigDecimal.valueOf(8));

        when(summaryRepository.findByScoreBetweenOrderByScoreDesc(
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(Arrays.asList(highScoreSummary));

        // When
        List<Summary> results = summaryService.getSummariesByScoreRange(7.0, 10.0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(BigDecimal.valueOf(8), results.get(0).getScore());
    }

    @Test
    void testPrintApiUsageStats() {
        // When
        summaryService.printApiUsageStats();

        // Then
        verify(usageMonitoringService).printStats();
    }

    @Test
    void testGetApiUsageStats() {
        // Given
        ApiUsageMonitoringService.UsageStats mockStats = 
                new ApiUsageMonitoringService.UsageStats(10, 8, 2, 1000L, 300L, 50L, LocalDateTime.now());
        when(usageMonitoringService.getCurrentStats()).thenReturn(mockStats);

        // When
        ApiUsageMonitoringService.UsageStats result = summaryService.getApiUsageStats();

        // Then
        assertNotNull(result);
        assertEquals(10, result.totalRequests);
        assertEquals(8, result.successfulRequests);
        assertEquals(2, result.failedRequests);
    }

    @Test
    void testGenerateSummary_DefaultParametersMethod() {
        // Given
        lenient().when(summaryRepository.findByArticleAndModel(testArticle, DEFAULT_MODEL)).thenReturn(Optional.empty());
        lenient().when(openAiClient.isApiAvailable()).thenReturn(true);
        lenient().when(openAiClient.estimateTokens(anyString())).thenReturn(100);
        lenient().when(openAiClient.generateEconomicSummary(anyString(), anyString())).thenReturn(testAiResponse);
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - 기본 파라미터 메서드 테스트
        Summary result = summaryService.generateSummary(testArticle);

        // Then
        assertNotNull(result);
        assertEquals("경제 성장률 상승으로 시장 전망이 긍정적입니다.", result.getSummaryText());
        assertEquals(BigDecimal.valueOf(7), result.getScore());

        // Mock 검증
        verify(openAiClient).generateEconomicSummary(testArticle.getTitle(), testArticle.getContent());
        verify(summaryRepository).save(any(Summary.class));
    }

    @Test
    void testGenerateSummaries_DefaultParametersMethod() {
        // Given
        List<Article> articles = Arrays.asList(testArticle);
        lenient().when(summaryRepository.findByArticleAndModel(testArticle, DEFAULT_MODEL)).thenReturn(Optional.empty());
        lenient().when(openAiClient.isApiAvailable()).thenReturn(true);
        lenient().when(openAiClient.estimateTokens(anyString())).thenReturn(100);
        lenient().when(openAiClient.generateEconomicSummary(anyString(), anyString())).thenReturn(testAiResponse);
        lenient().when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - 기본 파라미터 메서드 테스트
        List<Summary> results = summaryService.generateSummaries(articles);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(BigDecimal.valueOf(7), results.get(0).getScore());

        // Mock 검증
        verify(openAiClient).generateEconomicSummary(testArticle.getTitle(), testArticle.getContent());
        verify(summaryRepository).save(any(Summary.class));
    }
}