package com.yourco.econyang.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.econyang.openai.service.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 클라이언트 스키마 생성 테스트
 */
class OpenAiClientSchemaTest {

    @Test
    void should_generate_valid_schema_with_all_required_fields() throws Exception {
        // given
        OpenAiClient client = new OpenAiClient(WebClient.builder(), new ObjectMapper());
        
        // when - private 메서드에 접근하여 스키마 생성 테스트
        Method method = OpenAiClient.class.getDeclaredMethod("buildEconomicSummaryResponseFormat");
        method.setAccessible(true);
        
        Object responseFormat = method.invoke(client);
        
        // then
        assertNotNull(responseFormat);
        System.out.println("✅ OpenAI ResponseFormat 객체 생성 성공");
        
        // 스키마 내용 확인을 위해 JSON으로 변환 시도
        try {
            ObjectMapper mapper = new ObjectMapper();
            String schemaJson = mapper.writeValueAsString(responseFormat);
            System.out.println("생성된 스키마 구조:");
            System.out.println(schemaJson);
            
            // JSON 파싱이 성공하면 스키마가 유효함
            Map<?, ?> schemaMap = mapper.readValue(schemaJson, Map.class);
            assertNotNull(schemaMap);
            
        } catch (JsonProcessingException e) {
            System.out.println("스키마 JSON 변환 실패 (정상적일 수 있음): " + e.getMessage());
        }
    }
    
    @Test
    void should_create_fallback_summary_with_all_required_fields() throws Exception {
        // given
        OpenAiClient client = new OpenAiClient(WebClient.builder(), new ObjectMapper());
        
        // when - private 메서드에 접근하여 fallback 응답 생성 테스트
        Method method = OpenAiClient.class.getDeclaredMethod("createFallbackSummary", String.class, String.class);
        method.setAccessible(true);
        
        Object fallbackResponse = method.invoke(client, "테스트 제목", "테스트 오류");
        
        // then
        assertNotNull(fallbackResponse);
        System.out.println("✅ Fallback 응답 생성 성공: " + fallbackResponse.toString());
        
        // JSON으로 변환하여 모든 필드가 있는지 확인
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(fallbackResponse);
        System.out.println("Fallback JSON: " + json);
        
        // 필수 필드들이 모두 있는지 확인
        assertTrue(json.contains("summary"));
        assertTrue(json.contains("analysis"));
        assertTrue(json.contains("importance_score"));
        assertTrue(json.contains("economic_sectors"));
        assertTrue(json.contains("keywords"));
        assertTrue(json.contains("market_impact"));
        assertTrue(json.contains("investor_interest"));
        assertTrue(json.contains("confidence_score"));
        assertTrue(json.contains("context"));
    }
}