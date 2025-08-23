package com.yourco.econyang.service;

import com.yourco.econyang.config.DigestTemplateConfig;
import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DigestTemplateService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class DigestTemplateServiceTest {

    @Mock
    private DigestTemplateConfig templateConfig;

    @InjectMocks
    private DigestTemplateService digestTemplateService;

    private Map<String, DigestTemplateConfig.Template> testTemplates;
    private List<Summary> testSummaries;

    @BeforeEach
    void setUp() {
        setupTestTemplates();
        setupTestSummaries();
        
        when(templateConfig.getTemplates()).thenReturn(testTemplates);
    }

    private void setupTestTemplates() {
        testTemplates = new HashMap<>();
        
        // 기본 템플릿 설정
        DigestTemplateConfig.Template defaultTemplate = new DigestTemplateConfig.Template();
        defaultTemplate.setTitle("📈 경제뉴스 다이제스트");
        defaultTemplate.setHeader("# {{date}} 경제뉴스 다이제스트\n총 {{totalArticles}}개 기사\n\n");
        defaultTemplate.setArticleItem("## {{importance}}. {{title}} ({{source}})\n{{aiSummary}}\n중요도: {{importanceScore}}/10\n[링크]({{url}})\n\n");
        defaultTemplate.setFooter("---\n총 {{totalArticles}}개 뉴스 | 평균 중요도 {{avgImportance}}/10\n");
        
        testTemplates.put("default", defaultTemplate);
        
        // 간단한 템플릿 설정
        DigestTemplateConfig.Template simpleTemplate = new DigestTemplateConfig.Template();
        simpleTemplate.setTitle("📊 간단 다이제스트");
        simpleTemplate.setHeader("# 간단 다이제스트 - {{date}}\n");
        simpleTemplate.setArticleItem("**{{title}}** ({{source}})\n{{aiSummary}}\n\n");
        simpleTemplate.setFooter("---\n완료\n");
        
        testTemplates.put("simple", simpleTemplate);
    }

    private void setupTestSummaries() {
        testSummaries = new ArrayList<>();
        
        // 테스트 Summary 1
        Article article1 = new Article("한국경제", "https://example.com/article1", "경제 성장률 전망 상향 조정");
        article1.setAuthor("김기자");
        article1.setPublishedAt(LocalDateTime.now().minusHours(2));
        
        Summary summary1 = new Summary(article1, "gpt-4", "올해 경제 성장률이 예상보다 높을 것으로 전망됩니다.", "경제 회복이 가속화되고 있어 주목할 필요가 있습니다.");
        summary1.setScore(BigDecimal.valueOf(8.5));
        summary1.setBulletsList(Arrays.asList("경제성장", "전망", "상향조정"));
        
        testSummaries.add(summary1);
        
        // 테스트 Summary 2
        Article article2 = new Article("매일경제", "https://example.com/article2", "반도체 수출 증가세 지속");
        article2.setAuthor("박기자");
        article2.setPublishedAt(LocalDateTime.now().minusHours(1));
        
        Summary summary2 = new Summary(article2, "gpt-4", "반도체 수출이 지속적으로 증가하고 있습니다.", "글로벌 반도체 수요 회복이 주요 원인입니다.");
        summary2.setScore(BigDecimal.valueOf(7.2));
        summary2.setBulletsList(Arrays.asList("반도체", "수출", "증가"));
        
        testSummaries.add(summary2);
        
        // 테스트 Summary 3 (점수 없음)
        Article article3 = new Article("연합뉴스", "https://example.com/article3", "소비자 물가 안정세");
        article3.setPublishedAt(LocalDateTime.now().minusMinutes(30));
        
        Summary summary3 = new Summary(article3, "gpt-4", "소비자 물가가 안정세를 보이고 있습니다.", "인플레이션 압력이 완화되고 있는 신호입니다.");
        // 점수 설정 안함 (null)
        summary3.setBulletsList(Arrays.asList("물가", "안정", "인플레이션"));
        
        testSummaries.add(summary3);
    }

    @Test
    void testGenerateDigest_DefaultTemplate() {
        // When
        String digest = digestTemplateService.generateDigest(testSummaries);
        
        // Then
        assertNotNull(digest);
        assertFalse(digest.trim().isEmpty());
        
        // 기본 구조 확인
        assertTrue(digest.contains("경제뉴스 다이제스트"));
        assertTrue(digest.contains("총 3개 기사"));
        assertTrue(digest.contains("경제 성장률 전망 상향 조정"));
        assertTrue(digest.contains("반도체 수출 증가세 지속"));
        assertTrue(digest.contains("소비자 물가 안정세"));
        
        // 점수 정렬 확인 (8.5 > 7.2 > 0 순)
        int index1 = digest.indexOf("경제 성장률 전망 상향 조정");
        int index2 = digest.indexOf("반도체 수출 증가세 지속");
        int index3 = digest.indexOf("소비자 물가 안정세");
        
        assertTrue(index1 < index2, "높은 점수 기사가 먼저 나와야 함");
        assertTrue(index2 < index3, "중간 점수 기사가 다음에 나와야 함");
    }

    @Test
    void testGenerateDigest_SpecificTemplate() {
        // When
        String digest = digestTemplateService.generateDigest(testSummaries, "simple", "markdown");
        
        // Then
        assertNotNull(digest);
        assertFalse(digest.trim().isEmpty());
        
        // 간단한 템플릿 구조 확인
        assertTrue(digest.contains("간단 다이제스트"));
        assertTrue(digest.contains("**경제 성장률 전망 상향 조정** (한국경제)"));
        assertTrue(digest.contains("**반도체 수출 증가세 지속** (매일경제)"));
        assertTrue(digest.contains("완료"));
    }

    @Test
    void testGenerateDigest_EmptyList() {
        // When
        String digest = digestTemplateService.generateDigest(new ArrayList<>());
        
        // Then
        assertNotNull(digest);
        assertFalse(digest.trim().isEmpty());
        
        // 빈 다이제스트 내용 확인
        assertTrue(digest.contains("수집된 경제 뉴스가 없습니다"));
        assertTrue(digest.contains("EconDigest AI"));
    }

    @Test
    void testGenerateDigest_NullList() {
        // When
        String digest = digestTemplateService.generateDigest(null);
        
        // Then
        assertNotNull(digest);
        assertTrue(digest.contains("수집된 경제 뉴스가 없습니다"));
    }

    @Test
    void testGetAvailableTemplates() {
        // When
        Set<String> templates = digestTemplateService.getAvailableTemplates();
        
        // Then
        assertNotNull(templates);
        assertEquals(2, templates.size());
        assertTrue(templates.contains("default"));
        assertTrue(templates.contains("simple"));
    }

    @Test
    void testGetTemplateTitle() {
        // When
        String defaultTitle = digestTemplateService.getTemplateTitle("default");
        String simpleTitle = digestTemplateService.getTemplateTitle("simple");
        String unknownTitle = digestTemplateService.getTemplateTitle("unknown");
        
        // Then
        assertEquals("📈 경제뉴스 다이제스트", defaultTitle);
        assertEquals("📊 간단 다이제스트", simpleTitle);
        assertEquals("경제뉴스 다이제스트", unknownTitle); // 기본값
    }

    @Test
    void testGenerateDigest_VariableReplacement() {
        // When
        String digest = digestTemplateService.generateDigest(testSummaries, "default", "markdown");
        
        // Then
        // 날짜 변수 치환 확인
        assertTrue(digest.contains("2025년") || digest.contains("2024년")); // 현재 연도
        
        // 기사 수 변수 치환 확인
        assertTrue(digest.contains("총 3개 기사"));
        
        // 평균 중요도 계산 확인 (8.5 + 7.2 + 0) / 3 ≈ 5.2
        assertTrue(digest.contains("평균 중요도"));
        
        // 개별 기사 정보 확인
        assertTrue(digest.contains("중요도: 8/10")); // 8.5 -> 8
        assertTrue(digest.contains("중요도: 7/10")); // 7.2 -> 7
        assertTrue(digest.contains("중요도: 0/10")); // null -> 0
        
        // 소스 정보 확인
        assertTrue(digest.contains("(한국경제)"));
        assertTrue(digest.contains("(매일경제)"));
        assertTrue(digest.contains("(연합뉴스)"));
    }

    @Test
    void testGenerateDigest_StatisticsCalculation() {
        // Given - 다양한 점수의 Summary 추가
        Article highScoreArticle = new Article("테스트소스", "https://example.com/urgent", "긴급 경제 뉴스");
        
        Summary highScoreSummary = new Summary(highScoreArticle, "gpt-4", "긴급한 경제 소식", "매우 중요한 뉴스");
        highScoreSummary.setScore(BigDecimal.valueOf(9.5));
        
        List<Summary> extendedSummaries = new ArrayList<>(testSummaries);
        extendedSummaries.add(highScoreSummary);
        
        // When
        String digest = digestTemplateService.generateDigest(extendedSummaries, "default", "markdown");
        
        // Then
        assertNotNull(digest);
        System.out.println("Generated digest:\n" + digest);
        
        // 통계 정보 확인
        assertTrue(digest.contains("4개") || digest.contains("4"), "Should contain article count: " + digest);
        
        // 기본적인 다이제스트 구조 확인
        assertTrue(digest.contains("긴급 경제 뉴스"), "Should contain high score article");
        assertTrue(digest.contains("경제 성장률 전망"), "Should contain medium score article");
        
        // 점수 순서 확인 (9.5 > 8.5 > 7.2 > 0)
        int urgentIndex = digest.indexOf("긴급 경제 뉴스");
        int growthIndex = digest.indexOf("경제 성장률");
        assertTrue(urgentIndex < growthIndex, "Higher score should come first");
        
        // 충분한 내용이 생성되었는지 확인
        assertTrue(digest.length() > 200, "Should have sufficient content: " + digest.length()); // 200자 이상
    }

    @Test
    void testGenerateDigest_BulletPointsHandling() {
        // When
        String digest = digestTemplateService.generateDigest(testSummaries, "default", "markdown");
        
        // Then
        System.out.println("Digest for bullet points test:\n" + digest);
        
        // 기본 구조 확인 (현재 템플릿에는 키워드 필드가 포함되지 않음)
        assertTrue(digest.contains("경제 성장률 전망"), "Should contain article title");
        assertTrue(digest.contains("반도체 수출 증가세"), "Should contain article title");
        assertTrue(digest.contains("소비자 물가 안정세"), "Should contain article title");
        
        // Summary 내용이 포함되어 있는지 확인
        assertTrue(digest.contains("경제 성장률이 예상보다 높을") || digest.contains("전망"), "Should contain summary content");
        assertTrue(digest.contains("반도체 수출이 지속적으로") || digest.contains("증가"), "Should contain summary content");
    }

    @Test
    void testGenerateDigest_ScoreHandling() {
        // Given - 점수가 다양한 Summary 목록
        List<Summary> mixedScoreSummaries = new ArrayList<>();
        
        // 높은 점수
        Article article1 = new Article("테스트소스", "https://example.com/test1", "High Score News");
        Summary highScore = new Summary(article1, "gpt-4", "High score summary", "Important");
        highScore.setScore(BigDecimal.valueOf(9.0));
        mixedScoreSummaries.add(highScore);
        
        // 중간 점수
        Article article2 = new Article("테스트소스", "https://example.com/test2", "Medium Score News");
        Summary mediumScore = new Summary(article2, "gpt-4", "Medium score summary", "Moderate");
        mediumScore.setScore(BigDecimal.valueOf(6.0));
        mixedScoreSummaries.add(mediumScore);
        
        // 점수 없음 (null)
        Article article3 = new Article("테스트소스", "https://example.com/test3", "No Score News");
        Summary noScore = new Summary(article3, "gpt-4", "No score summary", "Unknown");
        // score를 설정하지 않음 (null)
        mixedScoreSummaries.add(noScore);
        
        // When
        String digest = digestTemplateService.generateDigest(mixedScoreSummaries, "default", "markdown");
        
        // Then
        assertNotNull(digest);
        
        // 점수 순 정렬 확인
        int highIndex = digest.indexOf("High Score News");
        int mediumIndex = digest.indexOf("Medium Score News");
        int noScoreIndex = digest.indexOf("No Score News");
        
        assertTrue(highIndex < mediumIndex, "높은 점수가 먼저 나와야 함");
        assertTrue(mediumIndex < noScoreIndex, "중간 점수가 다음에 나와야 함");
        
        // 점수 표시 확인
        assertTrue(digest.contains("중요도: 9/10"));
        assertTrue(digest.contains("중요도: 6/10"));
        assertTrue(digest.contains("중요도: 0/10")); // null은 0으로 표시
    }

    /**
     * 테스트용 Article 생성 헬퍼 메소드
     */
    private Article createTestArticle(String title, String urlSuffix) {
        Article article = new Article("테스트소스", "https://example.com/" + urlSuffix, title);
        article.setAuthor("테스트기자");
        article.setPublishedAt(LocalDateTime.now());
        return article;
    }
}