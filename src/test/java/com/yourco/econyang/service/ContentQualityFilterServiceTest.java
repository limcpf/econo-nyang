package com.yourco.econyang.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContentQualityFilterServiceTest {

    private ContentQualityFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new ContentQualityFilterService();
    }

    @Test
    void should_reject_content_with_subscription_keywords() {
        String title = "경제 뉴스 제목";
        String content = "이 기사의 전체 내용을 보려면 무료 구독하세요. 지금 가입하시면 다양한 혜택을 받을 수 있습니다.";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertFalse(result.isQualified());
        assertTrue(result.getReason().contains("구독"));
    }

    @Test
    void should_reject_promotional_content() {
        String title = "경제 분석 기사";
        String content = "경제 상황을 분석해드립니다. 50% 할인 혜택으로 프리미엄 구독하세요! 지금 클릭하여 자세히 보기.";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertFalse(result.isQualified());
        assertTrue(result.getReason().contains("광고성") || result.getReason().contains("홍보성"));
    }

    @Test
    void should_reject_too_short_content() {
        String title = "짧은 기사";
        String content = "너무 짧은 내용";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertFalse(result.isQualified());
        assertTrue(result.getReason().contains("너무 짧음"));
    }

    @Test
    void should_reject_unrelated_content() {
        String title = "경제 성장률 발표";
        String content = "오늘 날씨는 맑고 화창합니다. 축구 경기가 있을 예정입니다. 새로운 레시피를 소개해드립니다. 이것은 경제와 전혀 관련이 없는 내용입니다. 게임 소식도 전해드립니다.";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertFalse(result.isQualified());
        assertTrue(result.getReason().contains("관련성") || result.getReason().contains("뉴스 외 콘텐츠"));
    }

    @Test
    void should_accept_valid_economic_news() {
        String title = "한국 경제 성장률 3.2% 달성";
        String content = "한국의 올해 경제 성장률이 3.2%를 기록했다고 발표되었습니다. " +
                        "이는 전년 대비 0.5% 상승한 수치로, 정부의 경제 정책이 효과를 거두고 있음을 시사합니다. " +
                        "특히 수출 증가와 내수 회복이 성장률 상승의 주요 요인으로 분석됩니다. " +
                        "전문가들은 향후에도 안정적인 경제 성장세가 지속될 것으로 전망한다고 밝혔습니다.";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertTrue(result.isQualified());
        assertEquals("품질 검사 통과", result.getReason());
    }

    @Test
    void should_calculate_title_content_relevance_correctly() {
        String title = "삼성전자 주가 상승";
        String content = "삼성전자의 주가가 오늘 5% 상승했습니다. 전자 업체의 실적 개선 소식이 주가 상승의 배경입니다.";

        ContentQualityFilterService.ContentQualityResult result = 
            filterService.checkContentQuality(title, content);

        assertTrue(result.isQualified());
    }

    @Test
    void should_handle_null_or_empty_inputs() {
        ContentQualityFilterService.ContentQualityResult result1 = 
            filterService.checkContentQuality(null, "내용");
        assertFalse(result1.isQualified());

        ContentQualityFilterService.ContentQualityResult result2 = 
            filterService.checkContentQuality("제목", null);
        assertFalse(result2.isQualified());

        ContentQualityFilterService.ContentQualityResult result3 = 
            filterService.checkContentQuality("", "내용");
        assertFalse(result3.isQualified());
    }
}