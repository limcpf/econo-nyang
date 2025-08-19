package com.yourco.econdigest.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.econdigest.openai.dto.ChatCompletionResponse;
import com.yourco.econdigest.openai.dto.ChatMessage;
import com.yourco.econdigest.openai.dto.EconomicSummaryResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiClientTest {

    private MockWebServer mockWebServer;
    private OpenAiClient openAiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        WebClient.Builder webClientBuilder = WebClient.builder();
        
        openAiClient = new OpenAiClient(webClientBuilder, objectMapper);
        
        // 테스트용 설정 주입
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        ReflectionTestUtils.setField(openAiClient, "apiBase", baseUrl);
        ReflectionTestUtils.setField(openAiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(openAiClient, "modelSmall", "gpt-4o-mini");
        ReflectionTestUtils.setField(openAiClient, "modelMain", "gpt-4o");
        ReflectionTestUtils.setField(openAiClient, "requestTimeoutSec", 30);
        ReflectionTestUtils.setField(openAiClient, "maxInputTokens", 5000);
        ReflectionTestUtils.setField(openAiClient, "maxOutputTokens", 900);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void generateEconomicSummary_Success() throws Exception {
        // Given
        String articleTitle = "한국은행 기준금리 인상";
        String articleContent = "한국은행이 기준금리를 0.25%p 인상하여 3.5%로 조정했다고 발표했습니다.";
        
        EconomicSummaryResponse expectedResponse = createMockEconomicSummaryResponse();
        ChatCompletionResponse mockApiResponse = createMockChatCompletionResponse(expectedResponse);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(mockApiResponse)));

        // When
        EconomicSummaryResponse result = openAiClient.generateEconomicSummary(articleTitle, articleContent);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse.getSummary(), result.getSummary());
        assertEquals(expectedResponse.getAnalysis(), result.getAnalysis());
        assertEquals(expectedResponse.getImportanceScore(), result.getImportanceScore());
        assertEquals(expectedResponse.getMarketImpact(), result.getMarketImpact());
        assertEquals(expectedResponse.getInvestorInterest(), result.getInvestorInterest());
        assertEquals(expectedResponse.getConfidenceScore(), result.getConfidenceScore());

        // API 호출 검증
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/chat/completions", request.getPath());
        assertEquals("Bearer test-api-key", request.getHeader(HttpHeaders.AUTHORIZATION));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void generateEconomicSummary_ApiKeyMissing() {
        // Given
        ReflectionTestUtils.setField(openAiClient, "apiKey", "");
        String articleTitle = "테스트 제목";
        String articleContent = "테스트 내용";

        // When
        EconomicSummaryResponse result = openAiClient.generateEconomicSummary(articleTitle, articleContent);

        // Then
        assertNotNull(result);
        assertTrue(result.getSummary().contains("OpenAI API 키가 설정되지 않았습니다"));
        assertEquals(Integer.valueOf(5), result.getImportanceScore());
        assertEquals("중립", result.getMarketImpact());
        assertEquals(Integer.valueOf(1), result.getConfidenceScore());
    }

    @Test
    void generateEconomicSummary_ApiError() {
        // Given
        String articleTitle = "테스트 제목";
        String articleContent = "테스트 내용";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setBody("Rate limit exceeded"));

        // When
        EconomicSummaryResponse result = openAiClient.generateEconomicSummary(articleTitle, articleContent);

        // Then
        assertNotNull(result);
        assertTrue(result.getSummary().contains("AI 요약을 생성할 수 없습니다"));
        assertEquals("중립", result.getMarketImpact());
        assertEquals(Integer.valueOf(1), result.getConfidenceScore());
    }

    @Test
    void generateSimpleSummary_Success() throws Exception {
        // Given
        String text = "긴 텍스트 내용입니다.";
        String expectedSummary = "간단한 요약 결과";
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        ChatMessage message = new ChatMessage("assistant", expectedSummary);
        choice.setMessage(message);
        mockResponse.setChoices(Arrays.asList(choice));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(mockResponse)));

        // When
        String result = openAiClient.generateSimpleSummary(text, 100);

        // Then
        assertEquals(expectedSummary, result);
    }

    @Test
    void generateSimpleSummary_ApiKeyMissing() {
        // Given
        ReflectionTestUtils.setField(openAiClient, "apiKey", null);
        String text = "테스트 텍스트";

        // When
        String result = openAiClient.generateSimpleSummary(text, 100);

        // Then
        assertTrue(result.contains("OpenAI API 키가 설정되지 않아"));
    }

    @Test
    void isApiAvailable_WithValidApiKey() {
        // Given
        ReflectionTestUtils.setField(openAiClient, "apiKey", "valid-key");

        // When & Then
        assertTrue(openAiClient.isApiAvailable());
    }

    @Test
    void isApiAvailable_WithMissingApiKey() {
        // Given
        ReflectionTestUtils.setField(openAiClient, "apiKey", "");

        // When & Then
        assertFalse(openAiClient.isApiAvailable());
    }

    @Test
    void estimateTokens_ValidText() {
        // Given
        String text = "한국어 텍스트입니다";

        // When
        int tokens = openAiClient.estimateTokens(text);

        // Then
        assertTrue(tokens > 0);
        assertEquals(text.length() / 2, tokens);
    }

    @Test
    void estimateTokens_NullText() {
        // When & Then
        assertEquals(0, openAiClient.estimateTokens(null));
    }

    @Test
    void generateEconomicSummary_LongContent() throws Exception {
        // Given - 매우 긴 본문 생성
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("긴 텍스트 내용 ");
        }
        
        String articleTitle = "긴 기사 제목";
        EconomicSummaryResponse expectedResponse = createMockEconomicSummaryResponse();
        ChatCompletionResponse mockApiResponse = createMockChatCompletionResponse(expectedResponse);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(mockApiResponse)));

        // When
        EconomicSummaryResponse result = openAiClient.generateEconomicSummary(
                articleTitle, longContent.toString());

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse.getSummary(), result.getSummary());
    }

    @Test
    void generateEconomicSummary_WithRetry() throws Exception {
        // Given
        String articleTitle = "테스트 제목";
        String articleContent = "테스트 내용";
        
        EconomicSummaryResponse expectedResponse = createMockEconomicSummaryResponse();
        ChatCompletionResponse mockApiResponse = createMockChatCompletionResponse(expectedResponse);

        // 첫 번째 요청은 실패 (502 - retryable error)
        mockWebServer.enqueue(new MockResponse().setResponseCode(502));
        
        // 두 번째 요청은 성공
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(mockApiResponse)));

        // When
        EconomicSummaryResponse result = openAiClient.generateEconomicSummary(articleTitle, articleContent);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse.getSummary(), result.getSummary());
        
        // 두 번의 요청이 발생했는지 확인
        assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    void economicSummaryResponse_ConvenienceMethods() {
        // Given
        EconomicSummaryResponse response = new EconomicSummaryResponse();
        
        // When & Then - High Importance
        response.setImportanceScore(8);
        assertTrue(response.isHighImportance());
        
        response.setImportanceScore(5);
        assertFalse(response.isHighImportance());

        // When & Then - High Confidence
        response.setConfidenceScore(9);
        assertTrue(response.isHighConfidence());
        
        response.setConfidenceScore(4);
        assertFalse(response.isHighConfidence());

        // When & Then - Market Impact
        response.setMarketImpact("상승");
        assertTrue(response.isPositiveMarketImpact());
        assertFalse(response.isNegativeMarketImpact());
        
        response.setMarketImpact("하락");
        assertFalse(response.isPositiveMarketImpact());
        assertTrue(response.isNegativeMarketImpact());
        
        response.setMarketImpact("중립");
        assertFalse(response.isPositiveMarketImpact());
        assertFalse(response.isNegativeMarketImpact());
    }

    // Helper methods
    private EconomicSummaryResponse createMockEconomicSummaryResponse() {
        EconomicSummaryResponse response = new EconomicSummaryResponse();
        response.setSummary("한국은행이 기준금리를 3.5%로 인상했습니다.");
        response.setAnalysis("인플레이션 억제를 위한 통화정책 변화로 해석됩니다.");
        response.setImportanceScore(8);
        response.setEconomicSectors(Arrays.asList("금융", "부동산"));
        response.setKeywords(Arrays.asList("기준금리", "인상", "한국은행"));
        response.setMarketImpact("상승");
        response.setInvestorInterest("높음");
        response.setConfidenceScore(9);
        response.setContext("통화정책 기조 변화의 신호로 판단됩니다.");
        return response;
    }

    private ChatCompletionResponse createMockChatCompletionResponse(EconomicSummaryResponse economicResponse) throws Exception {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-test");
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel("gpt-4o");
        
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        String jsonContent = objectMapper.writeValueAsString(economicResponse);
        ChatMessage message = new ChatMessage("assistant", jsonContent);
        choice.setMessage(message);
        
        response.setChoices(Arrays.asList(choice));
        
        // Usage 정보 설정
        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(150);
        usage.setCompletionTokens(200);
        usage.setTotalTokens(350);
        response.setUsage(usage);
        
        return response;
    }
}