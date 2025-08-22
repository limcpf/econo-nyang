package com.yourco.econyang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DiscordService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class DiscordServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    private ObjectMapper objectMapper;
    private DiscordService discordService;
    private static final String TEST_WEBHOOK_URL = "https://discord.com/api/webhooks/test";
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        discordService = new DiscordService(restTemplate, objectMapper, TEST_WEBHOOK_URL);
    }
    
    @Test
    void testIsConfigured_WithValidUrl() {
        assertTrue(discordService.isConfigured());
    }
    
    @Test
    void testIsConfigured_WithEmptyUrl() {
        DiscordService emptyUrlService = new DiscordService(restTemplate, objectMapper, "");
        assertFalse(emptyUrlService.isConfigured());
    }
    
    @Test
    void testIsConfigured_WithNullUrl() {
        DiscordService nullUrlService = new DiscordService(restTemplate, objectMapper, null);
        assertFalse(nullUrlService.isConfigured());
    }
    
    @Test
    void testSendMessage_Success() {
        // Given
        String message = "테스트 메시지";
        String username = "TestBot";
        
        when(restTemplate.exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
        
        // When
        boolean result = discordService.sendMessage(message, username);
        
        // Then
        assertTrue(result);
        verify(restTemplate, times(1)).exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class));
    }
    
    @Test
    void testSendMessage_EmptyMessage() {
        // When
        boolean result = discordService.sendMessage("", "TestBot");
        
        // Then
        assertFalse(result);
        verify(restTemplate, never()).exchange(any(), any(), any(), any(Class.class));
    }
    
    @Test
    void testSendMessage_NullMessage() {
        // When
        boolean result = discordService.sendMessage(null, "TestBot");
        
        // Then
        assertFalse(result);
        verify(restTemplate, never()).exchange(any(), any(), any(), any(Class.class));
    }
    
    @Test
    void testSendMessage_NoWebhookUrl() {
        // Given
        DiscordService noUrlService = new DiscordService(restTemplate, objectMapper, "");
        
        // When
        boolean result = noUrlService.sendMessage("test message", "TestBot");
        
        // Then
        assertFalse(result);
        verify(restTemplate, never()).exchange(any(), any(), any(), any(Class.class));
    }
    
    @Test
    void testSendMessage_HttpError() {
        // Given
        String message = "테스트 메시지";
        
        when(restTemplate.exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        
        // When
        boolean result = discordService.sendMessage(message, "TestBot");
        
        // Then
        assertFalse(result);
        // BAD_REQUEST는 재시도하지 않음 (Rate Limit가 아닌 경우)
        verify(restTemplate, times(1)).exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class));
    }
    
    @Test
    void testSendMessage_LongMessage() {
        // Given - 2000자를 초과하는 긴 메시지 (확실하게 분할되도록)
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 200; i++) {  // 더 많은 반복으로 확실히 2000자 초과
            longMessage.append("이것은 긴 메시지 테스트입니다. 메시지 분할 테스트를 위한 내용입니다. ");
        }
        String message = longMessage.toString(); // 약 10000자 이상
        
        when(restTemplate.exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
        
        // When
        boolean result = discordService.sendMessage(message, "TestBot");
        
        // Then
        assertTrue(result);
        // 메시지가 분할되어 여러 번 호출됨 (10000자는 최소 5개 파트로 분할)
        verify(restTemplate, atLeast(3)).exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class));
    }
    
    @Test
    void testSendMessage_WithNullUsername() {
        // Given
        String message = "테스트 메시지";
        
        when(restTemplate.exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
        
        // When
        boolean result = discordService.sendMessage(message, null);
        
        // Then
        assertTrue(result);
        verify(restTemplate, times(1)).exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class));
    }
    
    @Test
    void testSendMessage_RateLimit_Retry() {
        // Given
        String message = "테스트 메시지";
        
        when(restTemplate.exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK)); // 재시도 후 성공
        
        // When
        boolean result = discordService.sendMessage(message, "TestBot");
        
        // Then
        assertTrue(result);
        verify(restTemplate, times(2)).exchange(anyString(), any(org.springframework.http.HttpMethod.class), 
                any(), eq(String.class));
    }
}