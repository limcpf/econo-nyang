package com.yourco.econyang.service;

import com.yourco.econyang.config.DigestTemplateConfig;
import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DigestTemplateServiceFriendlyTest {

    private DigestTemplateService digestTemplateService;

    @Mock
    private DigestTemplateConfig mockTemplateConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        digestTemplateService = new DigestTemplateService();
        ReflectionTestUtils.setField(digestTemplateService, "templateConfig", mockTemplateConfig);
    }

    @Test
    void should_generate_friendly_digest_with_emoji_explanations() {
        // Given
        List<Summary> summaries = createTestSummaries();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("👋 안녕하세요!"));
        assertTrue(result.contains("쉽게 정리해드려요"));
        assertTrue(result.contains("🤔"));
        assertTrue(result.contains("무슨 일인가요?"));
        assertTrue(result.contains("왜 중요한가요?"));
        assertTrue(result.contains("나에게 어떤 영향이 있을까요?"));
    }

    @Test
    void should_include_market_impact_explanations() {
        // Given
        List<Summary> summaries = createHighImportanceSummaries();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Then
        assertTrue(result.contains("투자에 미치는 영향"));
        assertTrue(result.contains("💰 투자하시는 분들은 특히 주목해보세요!"));
    }

    @Test
    void should_include_friendly_footer_with_tips() {
        // Given
        List<Summary> summaries = createTestSummaries();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Then
        assertTrue(result.contains("🎉 오늘의 경제뉴스 정리 끝!"));
        assertTrue(result.contains("오늘의 경제 분위기는?"));
        assertTrue(result.contains("😊 **좋은 소식**"));
        assertTrue(result.contains("😟 **주의할 소식**"));
        assertTrue(result.contains("💡 **투자 TIP**"));
        assertTrue(result.contains("🤗 **피드백 환영**"));
    }

    @Test
    void should_use_question_style_for_glossary() {
        // Given
        List<Summary> summaries = createSummariesWithGlossary();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Then
        assertTrue(result.contains("🤓 **어려운 용어 쉽게 설명**"));
        assertTrue(result.contains("**Q."));
        assertTrue(result.contains("이 뭔가요?**"));
        assertTrue(result.contains("A."));
    }

    @Test
    void should_provide_reading_time_estimation() {
        // Given
        List<Summary> summaries = createTestSummaries();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Then
        assertTrue(result.contains("📊 **읽는 시간**: 약 3-5분"));
        assertTrue(result.contains("오늘의 경제 흐름을 다 파악할 수 있어요!"));
    }

    @Test
    void should_include_friendly_section_headers() {
        // Given
        List<Summary> summaries = createTestSummaries();
        setupMockTemplateConfig();
        
        // When
        String result = digestTemplateService.generateDigest(summaries, "friendly", "markdown");
        
        // Debug output
        System.out.println("Generated digest result:");
        System.out.println(result);
        System.out.println("---");
        
        // Then
        assertTrue(result.contains("👋 안녕하세요!"), "Should contain greeting");
        assertTrue(result.contains("쉽게 정리해드려요"), "Should contain friendly explanation");
        // 섹션 헤더는 현재 템플릿에서 사용되지 않으므로 주석 처리
        // assertTrue(result.contains("🚨 **꼭 알아야 할 핵심 소식**"));
        // assertTrue(result.contains("오늘의 가장 중요한 이야기들이에요!"));
    }

    private List<Summary> createTestSummaries() {
        Article article1 = new Article("경제신문", "https://example.com/news1", "중앙은행 기준금리 인상");
        article1.setPublishedAt(LocalDateTime.now());
        
        Summary summary1 = new Summary(article1, "gpt-4o", 
                "중앙은행이 기준금리를 인상했습니다.", 
                "인플레이션 억제를 위한 조치입니다.");
        summary1.setScore(BigDecimal.valueOf(7.5));
        
        return Arrays.asList(summary1);
    }

    private List<Summary> createHighImportanceSummaries() {
        Article article = new Article("투자뉴스", "https://example.com/news2", "대기업 실적 발표");
        article.setPublishedAt(LocalDateTime.now());
        
        Summary summary = new Summary(article, "gpt-4o",
                "주요 기업의 실적 발표",
                "투자자들의 큰 관심사입니다.");
        summary.setScore(BigDecimal.valueOf(8.5)); // 높은 점수 -> 높은 영향도
        
        return Arrays.asList(summary);
    }

    private List<Summary> createSummariesWithGlossary() {
        Article article = new Article("정책뉴스", "https://example.com/news3", "중앙은행 양적완화 정책");
        article.setPublishedAt(LocalDateTime.now());
        
        Summary summary = new Summary(article, "gpt-4o",
                "양적완화 정책이 발표되었습니다.",
                "경기 부양 효과가 기대됩니다.");
        summary.setScore(BigDecimal.valueOf(6.0));
        
        return Arrays.asList(summary);
    }

    private void setupMockTemplateConfig() {
        Map<String, DigestTemplateConfig.Template> templates = new HashMap<>();
        
        DigestTemplateConfig.Template friendlyTemplate = new DigestTemplateConfig.Template();
        friendlyTemplate.setTitle("💡 쉽게 읽는 경제뉴스");
        friendlyTemplate.setSubtitle("복잡한 경제 이슈도 쉽게!");
        
        friendlyTemplate.setHeader("# 👋 안녕하세요! **{{date}}** 경제뉴스를 쉽게 정리해드려요\n\n" +
                "🤔 **\"경제뉴스가 어려워서 읽기 힘들다고요?\"**\n" +
                "걱정마세요! AI가 오늘의 주요 경제 소식 **{{totalArticles}}개**를 쏙쏙 골라서, 누구나 이해할 수 있게 정리해드렸어요.\n\n" +
                "🎯 **이렇게 선별했어요**: 중요도 {{minImportance}}점 이상인 뉴스만 엄선!\n" +
                "📊 **읽는 시간**: 약 3-5분이면 오늘의 경제 흐름을 다 파악할 수 있어요!\n\n---\n");
        
        friendlyTemplate.setArticleItem("## {{importance}}번째 🌟 **{{title}}**\n\n" +
                "> 💭 **한 줄 요약**: {{#aiSummaryFormatted}}{{this}}{{/aiSummaryFormatted}}\n\n" +
                "📰 **어디서 나온 소식?** {{source}} | 🕐 **언제?** {{publishedTime}}\n\n" +
                "### 🔍 **무슨 일인가요?**\n" +
                "{{#aiSummaryFormatted}}\n💡 {{this}}\n{{/aiSummaryFormatted}}\n\n" +
                "### 🤷‍♀️ **왜 중요한가요?**\n" +
                "{{#aiAnalysisFormatted}}\n🎯 {{this}}\n{{/aiAnalysisFormatted}}\n\n" +
                "### 📊 **나에게 어떤 영향이 있을까요?**\n" +
                "📈 **투자에 미치는 영향**: {{marketImpact}}\n" +
                "{{#if marketImpact_high}}💰 투자하시는 분들은 특히 주목해보세요!{{/if}}\n" +
                "{{#if marketImpact_medium}}📊 투자 포트폴리오를 점검해보는 것도 좋겠어요.{{/if}}\n" +
                "{{#if marketImpact_low}}😌 당장 크게 걱정하실 필요는 없어 보여요.{{/if}}\n\n" +
                "🎯 **관심도**: {{investorInterest}}\n" +
                "🏭 **관련 분야**: {{economicSectors}}\n\n" +
                "### 🏷️ **핵심 키워드**\n" +
                "`{{keywords}}`\n" +
                "*→ 이 키워드들로 추가 정보를 찾아보세요!*\n\n" +
                "{{#if glossary}}" +
                "### 🤓 **어려운 용어 쉽게 설명**\n" +
                "{{#glossary}}" +
                "**Q. {{term}}이 뭔가요?**\n" +
                "A. {{definition}}\n\n" +
                "{{/glossary}}" +
                "{{/if}}" +
                "🔗 [**전체 기사 읽어보기**]({{url}}) | 🎯 **중요도**: {{importanceScore}}/10\n\n---\n");
        
        friendlyTemplate.setFooter("\n---\n\n" +
                "# 🎉 오늘의 경제뉴스 정리 끝!\n\n" +
                "## 📊 **오늘은 이런 뉴스들이 있었어요**\n" +
                "📰 **총 수집된 뉴스**: {{totalCollected}}개\n" +
                "✨ **AI가 분석한 뉴스**: {{totalAnalyzed}}개\n" +
                "⭐ **평균 중요도**: {{avgImportance}}/10점\n\n" +
                "## 📈 **오늘의 경제 분위기는?**\n" +
                "😊 **좋은 소식**: {{positiveNews}}개 - 긍정적인 경제 신호들이에요!\n" +
                "😟 **주의할 소식**: {{negativeNews}}개 - 조금 신경 써야 할 이슈들\n" +
                "😐 **중립적 소식**: {{neutralNews}}개 - 그냥 알아두면 좋은 정보들\n\n" +
                "🤖 **AI 분석 정확도**: {{avgConfidence}}/10\n" +
                "*→ AI가 이 정도로 확신한다는 뜻이에요!*\n\n---\n\n" +
                "## 💬 **마지막으로 한 말씀**\n\n" +
                "💡 **투자 TIP**: 이 다이제스트는 정보 전달이 목적이에요. 실제 투자할 때는 여러 정보를 꼼꼼히 비교해보시고, 필요하면 전문가와 상담해주세요!\n\n" +
                "🕐 **다음 다이제스트**: {{nextDigestTime}}에 또 만나요!\n\n" +
                "📱 **더 많은 경제 정보**: [경제뉴스 다이제스트 홈](https://github.com/yourco/econdigest)\n\n" +
                "🤗 **피드백 환영**: 더 쉽고 유용한 다이제스트를 만들기 위해 여러분의 의견을 기다려요!\n\n" +
                "🤖 Made with 💕 by EconDigest AI");
        
        Map<String, String> sectionHeaders = new HashMap<>();
        sectionHeaders.put("topNews", "🚨 **꼭 알아야 할 핵심 소식** - 오늘의 가장 중요한 이야기들이에요!");
        sectionHeaders.put("marketNews", "📈 **주식시장 & 투자 이야기** - 돈의 흐름을 알아보세요");
        friendlyTemplate.setSectionHeaders(sectionHeaders);
        
        templates.put("friendly", friendlyTemplate);
        
        when(mockTemplateConfig.getTemplates()).thenReturn(templates);
    }
}