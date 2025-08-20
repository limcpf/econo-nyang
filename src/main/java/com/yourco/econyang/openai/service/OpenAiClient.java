package com.yourco.econyang.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.econyang.openai.dto.ChatCompletionRequest;
import com.yourco.econyang.openai.dto.ChatCompletionResponse;
import com.yourco.econyang.openai.dto.ChatMessage;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI API 클라이언트 서비스
 */
@Service
public class OpenAiClient {
    
    private static final String CHAT_COMPLETIONS_URL = "/v1/chat/completions";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.openai.apiKey:}")
    private String apiKey;
    
    @Value("${app.openai.apiBase:https://api.openai.com}")
    private String apiBase;
    
    @Value("${app.openai.modelSmall:gpt-4o-mini}")
    private String modelSmall;
    
    @Value("${app.openai.modelMain:gpt-4o}")
    private String modelMain;
    
    @Value("${app.openai.requestTimeoutSec:30}")
    private int requestTimeoutSec;
    
    @Value("${app.openai.maxInputTokens:5000}")
    private int maxInputTokens;
    
    @Value("${app.openai.maxOutputTokens:900}")
    private int maxOutputTokens;
    
    @Autowired
    public OpenAiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    /**
     * 경제뉴스 요약 생성 (Structured Outputs 사용)
     */
    public EconomicSummaryResponse generateEconomicSummary(String articleTitle, String articleContent) {
        if (isApiKeyMissing()) {
            return createFallbackSummary(articleTitle, "OpenAI API 키가 설정되지 않았습니다.");
        }
        
        try {
            // 시스템 프롬프트
            String systemPrompt = buildSystemPrompt();
            
            // 사용자 프롬프트
            String userPrompt = buildUserPrompt(articleTitle, articleContent);
            
            // 메시지 구성
            List<ChatMessage> messages = Arrays.asList(
                ChatMessage.system(systemPrompt),
                ChatMessage.user(userPrompt)
            );
            
            // 요청 구성
            ChatCompletionRequest request = buildStructuredRequest(messages);
            
            // API 호출
            ChatCompletionResponse response = callChatCompletions(request);
            
            // 구조화된 응답 파싱
            return parseStructuredResponse(response);
            
        } catch (Exception e) {
            System.err.println("OpenAI API 호출 중 오류 발생: " + e.getMessage());
            return createFallbackSummary(articleTitle, e.getMessage());
        }
    }
    
    /**
     * 일반 텍스트 요약 생성 (간단한 요약용)
     */
    public String generateSimpleSummary(String text, int maxLength) {
        if (isApiKeyMissing()) {
            return "OpenAI API 키가 설정되지 않아 요약을 생성할 수 없습니다.";
        }
        
        try {
            String prompt = String.format(
                "다음 텍스트를 %d자 이내로 간단히 요약해주세요:\n\n%s",
                maxLength, text
            );
            
            List<ChatMessage> messages = Arrays.asList(
                ChatMessage.system("당신은 한국어 텍스트 요약 전문가입니다."),
                ChatMessage.user(prompt)
            );
            
            ChatCompletionRequest request = buildSimpleRequest(messages);
            ChatCompletionResponse response = callChatCompletions(request);
            
            return response.getFirstChoiceContent();
            
        } catch (Exception e) {
            System.err.println("간단 요약 생성 중 오류: " + e.getMessage());
            return "요약 생성에 실패했습니다: " + e.getMessage();
        }
    }
    
    /**
     * API 키 확인
     */
    public boolean isApiAvailable() {
        return !isApiKeyMissing();
    }
    
    /**
     * 토큰 사용량 추정 (대략적인 계산)
     */
    public int estimateTokens(String text) {
        // 한국어의 경우 대략 2-3자당 1토큰으로 추정
        return text != null ? (text.length() / 2) : 0;
    }
    
    // === Private Methods ===
    
    private boolean isApiKeyMissing() {
        return apiKey == null || apiKey.trim().isEmpty();
    }
    
    private String buildSystemPrompt() {
        return "당신은 한국의 경제 뉴스 분석 전문가입니다. " +
               "주어진 경제 뉴스를 분석하여 구조화된 요약과 해석을 제공합니다. " +
               "요약은 정확하고 객관적이어야 하며, 경제적 맥락과 시장 영향을 포함해야 합니다. " +
               "모든 응답은 제공된 JSON 스키마 형식을 정확히 따라야 합니다.";
    }
    
    private String buildUserPrompt(String title, String content) {
        // 토큰 제한을 고려하여 내용 길이 제한
        String limitedContent = content;
        if (estimateTokens(content) > maxInputTokens) {
            int maxLength = maxInputTokens * 2; // 대략적인 문자 수 계산
            limitedContent = content.length() > maxLength ? 
                content.substring(0, maxLength) + "..." : content;
        }
        
        return String.format(
            "다음 경제 뉴스를 분석해주세요:\n\n" +
            "제목: %s\n\n" +
            "본문:\n%s\n\n" +
            "위 기사를 바탕으로 구조화된 요약과 경제 분석을 제공해주세요.",
            title, limitedContent
        );
    }
    
    private ChatCompletionRequest buildStructuredRequest(List<ChatMessage> messages) {
        ChatCompletionRequest request = new ChatCompletionRequest(modelMain, messages);
        
        // 기본 설정
        request.setTemperature(0.3); // 일관성 있는 응답을 위해 낮은 temperature
        request.setMaxTokens(maxOutputTokens);
        request.setUser("econdigest-system");
        
        // Structured Outputs 설정
        request.setResponseFormat(buildEconomicSummaryResponseFormat());
        
        return request;
    }
    
    private ChatCompletionRequest buildSimpleRequest(List<ChatMessage> messages) {
        ChatCompletionRequest request = new ChatCompletionRequest(modelSmall, messages);
        
        request.setTemperature(0.5);
        request.setMaxTokens(300); // 간단한 요약용이므로 적은 토큰
        
        return request;
    }
    
    private ChatCompletionRequest.ResponseFormat buildEconomicSummaryResponseFormat() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("required", Arrays.asList(
            "summary", "analysis", "importance_score", "market_impact", 
            "investor_interest", "confidence_score"
        ));
        
        Map<String, Object> properties = new HashMap<>();
        
        // summary
        Map<String, Object> summaryProp = new HashMap<>();
        summaryProp.put("type", "string");
        summaryProp.put("description", "기사의 핵심 내용을 3-5문장으로 요약");
        properties.put("summary", summaryProp);
        
        // analysis
        Map<String, Object> analysisProp = new HashMap<>();
        analysisProp.put("type", "string");
        analysisProp.put("description", "경제적 맥락과 의미, 잠재적 영향에 대한 분석");
        properties.put("analysis", analysisProp);
        
        // importance_score
        Map<String, Object> importanceProp = new HashMap<>();
        importanceProp.put("type", "integer");
        importanceProp.put("minimum", 1);
        importanceProp.put("maximum", 10);
        importanceProp.put("description", "기사의 경제적 중요도 (1=낮음, 10=매우 높음)");
        properties.put("importance_score", importanceProp);
        
        // economic_sectors
        Map<String, Object> sectorsProp = new HashMap<>();
        sectorsProp.put("type", "array");
        Map<String, Object> sectorsItems = new HashMap<>();
        sectorsItems.put("type", "string");
        sectorsProp.put("items", sectorsItems);
        sectorsProp.put("description", "관련 경제 섹터 (예: 금융, 부동산, 제조업)");
        properties.put("economic_sectors", sectorsProp);
        
        // keywords
        Map<String, Object> keywordsProp = new HashMap<>();
        keywordsProp.put("type", "array");
        Map<String, Object> keywordsItems = new HashMap<>();
        keywordsItems.put("type", "string");
        keywordsProp.put("items", keywordsItems);
        keywordsProp.put("description", "핵심 키워드 (3-7개)");
        properties.put("keywords", keywordsProp);
        
        // market_impact
        Map<String, Object> marketProp = new HashMap<>();
        marketProp.put("type", "string");
        marketProp.put("enum", Arrays.asList("상승", "하락", "중립", "혼재"));
        marketProp.put("description", "예상되는 시장 영향");
        properties.put("market_impact", marketProp);
        
        // investor_interest
        Map<String, Object> interestProp = new HashMap<>();
        interestProp.put("type", "string");
        interestProp.put("enum", Arrays.asList("높음", "보통", "낮음"));
        interestProp.put("description", "투자자 관심도");
        properties.put("investor_interest", interestProp);
        
        // confidence_score
        Map<String, Object> confidenceProp = new HashMap<>();
        confidenceProp.put("type", "integer");
        confidenceProp.put("minimum", 1);
        confidenceProp.put("maximum", 10);
        confidenceProp.put("description", "분석의 신뢰도 (1=낮음, 10=매우 높음)");
        properties.put("confidence_score", confidenceProp);
        
        // context
        Map<String, Object> contextProp = new HashMap<>();
        contextProp.put("type", "string");
        contextProp.put("description", "추가적인 배경 정보나 컨텍스트");
        properties.put("context", contextProp);
        
        schema.put("properties", properties);
        
        ChatCompletionRequest.ResponseFormat.JsonSchema jsonSchema = 
            new ChatCompletionRequest.ResponseFormat.JsonSchema(
                "economic_summary",
                "경제 뉴스 요약 및 분석 결과",
                schema
            );
        
        return new ChatCompletionRequest.ResponseFormat(jsonSchema);
    }
    
    private ChatCompletionResponse callChatCompletions(ChatCompletionRequest request) {
        return webClient
                .post()
                .uri(apiBase + CHAT_COMPLETIONS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(requestTimeoutSec))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                    .filter(throwable -> isRetryableError(throwable)))
                .onErrorMap(WebClientResponseException.class, this::handleApiError)
                .block();
    }
    
    private EconomicSummaryResponse parseStructuredResponse(ChatCompletionResponse response) {
        try {
            String content = response.getFirstChoiceContent();
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("OpenAI 응답이 비어있습니다.");
            }
            
            return objectMapper.readValue(content, EconomicSummaryResponse.class);
            
        } catch (JsonProcessingException e) {
            System.err.println("구조화된 응답 파싱 실패: " + e.getMessage());
            throw new RuntimeException("응답 파싱 실패", e);
        }
    }
    
    private EconomicSummaryResponse createFallbackSummary(String title, String errorMessage) {
        EconomicSummaryResponse fallback = new EconomicSummaryResponse();
        fallback.setSummary("AI 요약을 생성할 수 없습니다: " + errorMessage);
        fallback.setAnalysis("자동 분석이 불가능한 상태입니다.");
        fallback.setImportanceScore(5); // 중간값
        fallback.setMarketImpact("중립");
        fallback.setInvestorInterest("보통");
        fallback.setConfidenceScore(1); // 낮은 신뢰도
        fallback.setContext("오류로 인한 기본 응답: " + errorMessage);
        return fallback;
    }
    
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            HttpStatus status = ex.getStatusCode();
            // 429 (Rate Limit), 502 (Bad Gateway), 503 (Service Unavailable), 504 (Gateway Timeout)
            return status == HttpStatus.TOO_MANY_REQUESTS || 
                   status == HttpStatus.BAD_GATEWAY ||
                   status == HttpStatus.SERVICE_UNAVAILABLE ||
                   status == HttpStatus.GATEWAY_TIMEOUT;
        }
        return false;
    }
    
    private RuntimeException handleApiError(WebClientResponseException ex) {
        String errorMessage = String.format(
            "OpenAI API 오류 [%d]: %s", 
            ex.getRawStatusCode(), 
            ex.getResponseBodyAsString()
        );
        
        System.err.println(errorMessage);
        return new RuntimeException(errorMessage, ex);
    }
}