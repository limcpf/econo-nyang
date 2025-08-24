package com.yourco.econyang.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 스키마 유효성 테스트
 */
class OpenAiSchemaValidationTest {

    @Test
    void should_serialize_complete_economic_summary_response() {
        // given
        EconomicSummaryResponse response = new EconomicSummaryResponse();
        response.setSummary("테스트 요약");
        response.setAnalysis("테스트 분석");
        response.setImportanceScore(7);
        response.setEconomicSectors(Arrays.asList("금융", "증권"));
        response.setKeywords(Arrays.asList("시장", "경제", "투자"));
        response.setMarketImpact("상승");
        response.setInvestorInterest("높음");
        response.setConfidenceScore(8);
        response.setContext("테스트 컨텍스트");
        
        // when & then - JSON 직렬화가 오류 없이 되어야 함
        ObjectMapper mapper = new ObjectMapper();
        assertDoesNotThrow(() -> {
            String json = mapper.writeValueAsString(response);
            System.out.println("직렬화된 JSON: " + json);
            
            // 역직렬화도 테스트
            EconomicSummaryResponse deserialized = mapper.readValue(json, EconomicSummaryResponse.class);
            assertEquals(response.getSummary(), deserialized.getSummary());
            assertEquals(response.getImportanceScore(), deserialized.getImportanceScore());
            assertNotNull(deserialized.getEconomicSectors());
            assertNotNull(deserialized.getKeywords());
            assertNotNull(deserialized.getContext());
        });
    }
    
    @Test
    void should_validate_required_fields_are_not_null() {
        // given
        EconomicSummaryResponse response = new EconomicSummaryResponse();
        response.setSummary("요약");
        response.setAnalysis("분석");
        response.setImportanceScore(5);
        response.setEconomicSectors(Arrays.asList("일반"));
        response.setKeywords(Arrays.asList("키워드"));
        response.setMarketImpact("중립");
        response.setInvestorInterest("보통");
        response.setConfidenceScore(5);
        response.setContext("컨텍스트");
        
        // when & then - 모든 필수 필드가 설정되어 있어야 함
        assertNotNull(response.getSummary());
        assertNotNull(response.getAnalysis());
        assertNotNull(response.getImportanceScore());
        assertNotNull(response.getEconomicSectors());
        assertNotNull(response.getKeywords());
        assertNotNull(response.getMarketImpact());
        assertNotNull(response.getInvestorInterest());
        assertNotNull(response.getConfidenceScore());
        assertNotNull(response.getContext());
        
        System.out.println("✅ 모든 필수 필드가 설정됨");
    }
}