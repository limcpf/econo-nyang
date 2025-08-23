package com.yourco.econyang.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI API 사용량 모니터링 서비스
 */
@Service
public class ApiUsageMonitoringService {
    
    // 사용량 카운터들
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    
    // 토큰 사용량 (추정치)
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    
    // 비용 추정 (USD 센트 단위)
    private final AtomicLong estimatedCostCents = new AtomicLong(0);
    
    // 모니터링 시작 시간
    private final LocalDateTime startTime = LocalDateTime.now();
    
    // 모델별 토큰 가격 (USD per 1M tokens)
    private static final double GPT4O_INPUT_PRICE = 5.00;  // $5.00 per 1M input tokens
    private static final double GPT4O_OUTPUT_PRICE = 15.00; // $15.00 per 1M output tokens
    private static final double GPT4O_MINI_INPUT_PRICE = 0.15;  // $0.15 per 1M input tokens  
    private static final double GPT4O_MINI_OUTPUT_PRICE = 0.60; // $0.60 per 1M output tokens
    
    /**
     * API 요청 기록
     */
    public void recordRequest(String model, int inputTokens, int outputTokens, boolean success, String errorMessage) {
        totalRequests.incrementAndGet();
        
        if (success) {
            successfulRequests.incrementAndGet();
            
            // 토큰 사용량 누적
            totalInputTokens.addAndGet(inputTokens);
            totalOutputTokens.addAndGet(outputTokens);
            
            // 비용 계산 및 누적
            long costCents = calculateCostCents(model, inputTokens, outputTokens);
            estimatedCostCents.addAndGet(costCents);
            
        } else {
            failedRequests.incrementAndGet();
            System.err.println("API 요청 실패: " + errorMessage);
        }
    }
    
    /**
     * 현재 사용량 통계 반환
     */
    public UsageStats getCurrentStats() {
        return new UsageStats(
            totalRequests.get(),
            successfulRequests.get(), 
            failedRequests.get(),
            totalInputTokens.get(),
            totalOutputTokens.get(),
            estimatedCostCents.get(),
            startTime
        );
    }
    
    /**
     * 사용량 통계 출력
     */
    public void printStats() {
        UsageStats stats = getCurrentStats();
        
        System.out.println("=== OpenAI API 사용량 통계 ===");
        System.out.println("모니터링 시작: " + stats.startTime);
        System.out.println("총 요청 수: " + stats.totalRequests);
        System.out.println("성공: " + stats.successfulRequests + ", 실패: " + stats.failedRequests);
        
        if (stats.totalRequests > 0) {
            double successRate = (double) stats.successfulRequests / stats.totalRequests * 100;
            System.out.println("성공률: " + String.format("%.1f", successRate) + "%");
        }
        
        System.out.println("토큰 사용량:");
        System.out.println("  - 입력: " + String.format("%,d", stats.totalInputTokens) + " tokens");
        System.out.println("  - 출력: " + String.format("%,d", stats.totalOutputTokens) + " tokens");
        System.out.println("  - 총계: " + String.format("%,d", stats.totalInputTokens + stats.totalOutputTokens) + " tokens");
        
        double estimatedCostUSD = stats.estimatedCostCents / 100.0;
        System.out.println("예상 비용: $" + String.format("%.4f", estimatedCostUSD));
        System.out.println("============================");
    }
    
    /**
     * 사용량 초기화
     */
    public void resetStats() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        estimatedCostCents.set(0);
        
        System.out.println("API 사용량 통계가 초기화되었습니다.");
    }
    
    /**
     * 일일 예산 초과 여부 확인 (센트 단위)
     */
    public boolean isDailyBudgetExceeded(long dailyBudgetCents) {
        return estimatedCostCents.get() > dailyBudgetCents;
    }
    
    /**
     * 비용 추정 (센트 단위로 반환)
     */
    private long calculateCostCents(String model, int inputTokens, int outputTokens) {
        double inputPrice, outputPrice;
        
        if (model.contains("gpt-4o-mini")) {
            inputPrice = GPT4O_MINI_INPUT_PRICE;
            outputPrice = GPT4O_MINI_OUTPUT_PRICE;
        } else if (model.contains("gpt-4o")) {
            inputPrice = GPT4O_INPUT_PRICE;
            outputPrice = GPT4O_OUTPUT_PRICE;
        } else {
            // 기본값으로 gpt-4o-mini 가격 사용
            inputPrice = GPT4O_MINI_INPUT_PRICE;
            outputPrice = GPT4O_MINI_OUTPUT_PRICE;
        }
        
        // 1M 토큰당 가격을 개별 토큰 가격으로 변환하고 센트로 변환
        double inputCost = (inputTokens / 1_000_000.0) * inputPrice * 100;
        double outputCost = (outputTokens / 1_000_000.0) * outputPrice * 100;
        
        return Math.round(inputCost + outputCost);
    }
    
    /**
     * 사용량 통계 DTO
     */
    public static class UsageStats {
        public final int totalRequests;
        public final int successfulRequests;
        public final int failedRequests;
        public final long totalInputTokens;
        public final long totalOutputTokens;
        public final long estimatedCostCents;
        public final LocalDateTime startTime;
        
        public UsageStats(int totalRequests, int successfulRequests, int failedRequests,
                         long totalInputTokens, long totalOutputTokens, 
                         long estimatedCostCents, LocalDateTime startTime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.estimatedCostCents = estimatedCostCents;
            this.startTime = startTime;
        }
        
        public double getEstimatedCostUSD() {
            return estimatedCostCents / 100.0;
        }
        
        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "UsageStats{requests=%d, success=%d, failed=%d, tokens=%d, cost=$%.4f}",
                totalRequests, successfulRequests, failedRequests,
                totalInputTokens + totalOutputTokens, getEstimatedCostUSD()
            );
        }
    }
}