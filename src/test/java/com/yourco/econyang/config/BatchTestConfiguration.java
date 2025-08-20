package com.yourco.econyang.config;

import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * 테스트용 설정 클래스
 */
@TestConfiguration
public class BatchTestConfiguration {

    /**
     * 테스트용 Mock OpenAiClient
     */
    @Bean
    @Primary
    public OpenAiClient mockOpenAiClient() {
        OpenAiClient mockClient = Mockito.mock(OpenAiClient.class);
        
        // generateEconomicSummary 메서드 Mock
        EconomicSummaryResponse mockResponse = new EconomicSummaryResponse();
        mockResponse.setSummary("테스트 요약: 경제뉴스 테스트");
        mockResponse.setAnalysis("테스트 분석: 이 기사는 경제에 미치는 영향이 제한적입니다.");
        mockResponse.setImportanceScore(5);
        mockResponse.setEconomicSectors(Arrays.asList("금융", "테스트"));
        mockResponse.setKeywords(Arrays.asList("테스트", "경제", "뉴스"));
        mockResponse.setMarketImpact("중립");
        mockResponse.setInvestorInterest("보통");
        mockResponse.setConfidenceScore(7);
        mockResponse.setContext("테스트 환경에서 생성된 Mock 응답입니다.");
        
        Mockito.when(mockClient.generateEconomicSummary(anyString(), anyString()))
                .thenReturn(mockResponse);
        
        // generateSimpleSummary 메서드 Mock
        Mockito.when(mockClient.generateSimpleSummary(anyString(), anyInt()))
                .thenReturn("테스트 간단 요약");
        
        // isApiAvailable 메서드 Mock
        Mockito.when(mockClient.isApiAvailable())
                .thenReturn(true);
        
        // estimateTokens 메서드 Mock
        Mockito.when(mockClient.estimateTokens(anyString()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(0);
                    return text != null ? text.length() / 2 : 0;
                });
        
        return mockClient;
    }
}