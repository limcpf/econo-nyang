package com.yourco.econyang.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 스마트 날짜 필터링 성능 모니터링 유틸리티
 */
@Component
public class PerformanceMonitor {
    
    private final ConcurrentHashMap<String, AtomicLong> timings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> successCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    
    /**
     * 작업 시간 측정 시작
     */
    public long startTiming(String operation) {
        return System.currentTimeMillis();
    }
    
    /**
     * 작업 시간 측정 종료 및 기록
     */
    public void endTiming(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        timings.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(duration);
        counters.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * 성공 카운트 증가
     */
    public void recordSuccess(String operation) {
        successCounts.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * 실패 카운트 증가
     */
    public void recordFailure(String operation) {
        failureCounts.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * 성능 통계 출력
     */
    public void printPerformanceStats() {
        System.out.println("=== 스마트 날짜 필터링 성능 통계 ===");
        System.out.println("수집 시각: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        // 작업별 평균 처리 시간
        for (String operation : timings.keySet()) {
            long totalTime = timings.get(operation).get();
            int count = counters.get(operation).get();
            int successes = successCounts.getOrDefault(operation, new AtomicInteger(0)).get();
            int failures = failureCounts.getOrDefault(operation, new AtomicInteger(0)).get();
            
            if (count > 0) {
                double avgTime = (double) totalTime / count;
                double successRate = count > 0 ? (double) successes / count * 100 : 0;
                
                System.out.println(String.format(
                    "[%s] 평균 시간: %.2fms, 총 횟수: %d, 성공률: %.1f%% (%d성공/%d실패)",
                    operation, avgTime, count, successRate, successes, failures
                ));
            }
        }
        
        System.out.println();
    }
    
    /**
     * 통계 초기화
     */
    public void reset() {
        timings.clear();
        counters.clear();
        successCounts.clear();
        failureCounts.clear();
        System.out.println("성능 통계가 초기화되었습니다.");
    }
    
    /**
     * 특정 작업의 통계 조회
     */
    public String getOperationStats(String operation) {
        long totalTime = timings.getOrDefault(operation, new AtomicLong(0)).get();
        int count = counters.getOrDefault(operation, new AtomicInteger(0)).get();
        int successes = successCounts.getOrDefault(operation, new AtomicInteger(0)).get();
        int failures = failureCounts.getOrDefault(operation, new AtomicInteger(0)).get();
        
        if (count == 0) {
            return String.format("[%s] 통계 없음", operation);
        }
        
        double avgTime = (double) totalTime / count;
        double successRate = (double) successes / count * 100;
        
        return String.format(
            "[%s] 평균: %.2fms, 횟수: %d, 성공률: %.1f%%",
            operation, avgTime, count, successRate
        );
    }
}