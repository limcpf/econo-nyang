package com.yourco.econyang.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Discord Webhook 발송 서비스
 */
@Service
public class DiscordService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    
    // Discord 메시지 최대 길이 (2000자)
    private static final int MAX_MESSAGE_LENGTH = 2000;
    
    // Rate Limit 재시도 설정
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    
    public DiscordService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${discord.webhook.url:}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
    }
    
    /**
     * Discord 채널에 메시지 발송
     * 
     * @param message 발송할 메시지 (마크다운 형식)
     * @param username 표시될 사용자명 (선택사항)
     * @return 발송 성공 여부
     */
    public boolean sendMessage(String message, String username) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            System.err.println("Discord Webhook URL이 설정되지 않았습니다. discord.webhook.url 환경변수를 확인하세요.");
            return false;
        }
        
        if (message == null || message.trim().isEmpty()) {
            System.err.println("발송할 메시지가 비어있습니다.");
            return false;
        }
        
        // 메시지 길이에 따라 분할
        List<String> messageParts = splitMessage(message);
        System.out.println("Discord 메시지 분할: " + messageParts.size() + "개 파트");
        
        boolean allSuccess = true;
        for (int i = 0; i < messageParts.size(); i++) {
            String part = messageParts.get(i);
            String partUsername = username;
            
            // 여러 파트인 경우 번호 표시
            if (messageParts.size() > 1) {
                partUsername = username + " (" + (i + 1) + "/" + messageParts.size() + ")";
            }
            
            boolean success = sendSingleMessage(part, partUsername);
            if (!success) {
                allSuccess = false;
            }
            
            // Discord Rate Limit 방지를 위한 지연 (파트 간)
            if (i < messageParts.size() - 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return allSuccess;
    }
    
    /**
     * 단일 메시지 발송 (재시도 포함)
     */
    private boolean sendSingleMessage(String message, String username) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                DiscordWebhookPayload payload = new DiscordWebhookPayload();
                payload.content = message;
                payload.username = username != null ? username : "EconDigest Bot";
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                        webhookUrl, HttpMethod.POST, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("Discord 메시지 발송 성공");
                    return true;
                } else {
                    System.err.println("Discord 메시지 발송 실패: HTTP " + response.getStatusCodeValue());
                    return false;
                }
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    // Rate Limit 처리
                    long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    System.err.println("Discord Rate Limit 도달. " + backoffMs + "ms 대기 후 재시도 (" + 
                                     attempt + "/" + MAX_RETRIES + ")");
                    
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    System.err.println("Discord API 오류: " + e.getStatusCode() + " - " + e.getMessage());
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Discord 메시지 발송 실패 (시도 " + attempt + "/" + MAX_RETRIES + "): " + 
                                 e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(INITIAL_BACKOFF_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        
        System.err.println("Discord 메시지 발송 최종 실패");
        return false;
    }
    
    /**
     * 긴 메시지를 Discord 제한에 맞게 분할
     * 마크다운 구조를 보존하면서 분할
     */
    private List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            parts.add(message);
            return parts;
        }
        
        // 줄 단위로 분할하여 마크다운 구조 보존
        String[] lines = message.split("\n");
        StringBuilder currentPart = new StringBuilder();
        
        for (String line : lines) {
            // 현재 파트에 이 줄을 추가했을 때의 길이 계산
            int newLength = currentPart.length() + line.length() + 1; // +1 for newline
            
            if (newLength > MAX_MESSAGE_LENGTH && currentPart.length() > 0) {
                // 현재 파트를 완성하고 새 파트 시작
                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
            }
            
            // 단일 줄이 너무 긴 경우 강제 분할
            if (line.length() > MAX_MESSAGE_LENGTH) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString().trim());
                    currentPart = new StringBuilder();
                }
                
                // 긴 줄을 청크로 분할
                for (int i = 0; i < line.length(); i += MAX_MESSAGE_LENGTH) {
                    String chunk = line.substring(i, Math.min(i + MAX_MESSAGE_LENGTH, line.length()));
                    parts.add(chunk);
                }
            } else {
                if (currentPart.length() > 0) {
                    currentPart.append("\n");
                }
                currentPart.append(line);
            }
        }
        
        // 마지막 파트 추가
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }
        
        return parts;
    }
    
    /**
     * Webhook URL 설정 상태 확인
     */
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty();
    }
    
    /**
     * Discord Webhook Payload 구조
     */
    private static class DiscordWebhookPayload {
        @JsonProperty("content")
        public String content;
        
        @JsonProperty("username")
        public String username;
    }
}