package com.yourco.econyang.service;

import com.yourco.econyang.dto.ArticleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 이진 탐색을 사용해서 24시간 이내 뉴스를 효율적으로 필터링하는 서비스
 * 발행일자가 없는 기사들을 언론사별로 그룹핑하고, 중앙부터 탐색해서 24시간 범위를 찾음
 */
@Service
public class BinarySearchDateFilterService {
    
    private final ContentDateExtractorService dateExtractor;
    private final ExecutorService executorService;
    
    @Autowired
    public BinarySearchDateFilterService(ContentDateExtractorService dateExtractor) {
        this.dateExtractor = dateExtractor;
        this.executorService = Executors.newFixedThreadPool(5); // 병렬 처리용
    }
    
    /**
     * 발행일자가 없는 기사들을 언론사별로 그룹핑하고 이진 탐색으로 24시간 이내 기사만 필터링
     */
    public List<ArticleDto> filterArticlesWithBinarySearch(List<ArticleDto> articlesWithoutDate, 
                                                          LocalDateTime cutoffTime) {
        
        System.out.println("=== 이진 탐색 날짜 필터링 시작 ===");
        System.out.println("발행일자 없는 기사 수: " + articlesWithoutDate.size());
        System.out.println("기준 시간: " + cutoffTime);
        
        // 언론사별로 그룹핑
        Map<String, List<ArticleDto>> articlesBySource = articlesWithoutDate.stream()
                .collect(Collectors.groupingBy(ArticleDto::getSource));
        
        System.out.println("언론사별 그룹: " + articlesBySource.keySet());
        
        List<CompletableFuture<List<ArticleDto>>> futures = new ArrayList<>();
        
        // 각 언론사별로 병렬 처리
        for (Map.Entry<String, List<ArticleDto>> entry : articlesBySource.entrySet()) {
            String sourceName = entry.getKey();
            List<ArticleDto> articles = entry.getValue();
            
            CompletableFuture<List<ArticleDto>> future = CompletableFuture.supplyAsync(() -> {
                return processArticlesBySourceWithBinarySearch(sourceName, articles, cutoffTime);
            }, executorService);
            
            futures.add(future);
        }
        
        // 모든 병렬 작업 완료 대기
        List<ArticleDto> validArticles = new ArrayList<>();
        for (CompletableFuture<List<ArticleDto>> future : futures) {
            try {
                validArticles.addAll(future.get());
            } catch (Exception e) {
                System.err.println("병렬 처리 오류: " + e.getMessage());
            }
        }
        
        System.out.println("이진 탐색 완료: " + validArticles.size() + "개 기사가 24시간 이내로 확인됨");
        return validArticles;
    }
    
    /**
     * 특정 언론사의 기사들을 이진 탐색으로 처리
     */
    private List<ArticleDto> processArticlesBySourceWithBinarySearch(String sourceName, 
                                                                   List<ArticleDto> articles, 
                                                                   LocalDateTime cutoffTime) {
        
        System.out.println(sourceName + ": " + articles.size() + "개 기사 처리 시작");
        
        if (articles.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ArticleDto> validArticles = new ArrayList<>();
        
        // 이진 탐색으로 24시간 이내 범위 찾기
        BinarySearchResult result = findValidRangeWithBinarySearch(sourceName, articles, cutoffTime);
        
        System.out.println(sourceName + ": 이진 탐색 결과 - " + 
                          result.validArticles.size() + "개 유효, " + 
                          result.invalidArticles.size() + "개 제외");
        
        return result.validArticles;
    }
    
    /**
     * 이진 탐색으로 24시간 이내 기사 범위 찾기
     */
    private BinarySearchResult findValidRangeWithBinarySearch(String sourceName, 
                                                            List<ArticleDto> articles, 
                                                            LocalDateTime cutoffTime) {
        
        List<ArticleDto> validArticles = new ArrayList<>();
        List<ArticleDto> invalidArticles = new ArrayList<>();
        
        int size = articles.size();
        
        // 중앙부터 시작해서 양방향으로 확장하면서 유효한 범위 찾기
        int center = size / 2;
        
        // 중앙 기사의 날짜 확인
        Optional<LocalDateTime> centerDate = extractDateSafely(articles.get(center), sourceName);
        
        if (!centerDate.isPresent()) {
            // 중앙 기사에서 날짜를 찾을 수 없으면 전체 기사 제외
            System.out.println(sourceName + ": 중앙 기사에서 날짜 추출 실패, 전체 제외");
            invalidArticles.addAll(articles);
            return new BinarySearchResult(validArticles, invalidArticles);
        }
        
        LocalDateTime centerDateTime = centerDate.get();
        boolean isCenterValid = centerDateTime.isAfter(cutoffTime);
        
        System.out.println(sourceName + ": 중앙 기사(" + center + ") 날짜: " + centerDateTime + 
                          ", 유효: " + isCenterValid);
        
        if (isCenterValid) {
            validArticles.add(articles.get(center));
            
            // 중앙이 유효하면 양방향으로 확장
            expandValidRange(articles, center, sourceName, cutoffTime, validArticles, invalidArticles);
            
        } else {
            invalidArticles.add(articles.get(center));
            
            // 중앙이 무효하면 더 최근 기사들(인덱스가 작은 쪽)에서 이진 탐색
            searchInRecentArticles(articles, 0, center - 1, sourceName, cutoffTime, validArticles, invalidArticles);
        }
        
        return new BinarySearchResult(validArticles, invalidArticles);
    }
    
    /**
     * 유효한 중앙 지점에서 양방향으로 확장
     */
    private void expandValidRange(List<ArticleDto> articles, int center, String sourceName, 
                                LocalDateTime cutoffTime, List<ArticleDto> validArticles, 
                                List<ArticleDto> invalidArticles) {
        
        // 왼쪽으로 확장 (더 최근 기사들)
        for (int i = center - 1; i >= 0; i--) {
            Optional<LocalDateTime> date = extractDateSafely(articles.get(i), sourceName);
            if (date.isPresent() && date.get().isAfter(cutoffTime)) {
                validArticles.add(articles.get(i));
            } else {
                invalidArticles.add(articles.get(i));
                break; // 연속성이 깨지면 중단
            }
        }
        
        // 오른쪽으로 확장 (더 오래된 기사들)
        for (int i = center + 1; i < articles.size(); i++) {
            Optional<LocalDateTime> date = extractDateSafely(articles.get(i), sourceName);
            if (date.isPresent() && date.get().isAfter(cutoffTime)) {
                validArticles.add(articles.get(i));
            } else {
                invalidArticles.add(articles.get(i));
                break; // 연속성이 깨지면 중단
            }
        }
    }
    
    /**
     * 최근 기사들 영역에서 이진 탐색
     */
    private void searchInRecentArticles(List<ArticleDto> articles, int left, int right, 
                                       String sourceName, LocalDateTime cutoffTime,
                                       List<ArticleDto> validArticles, List<ArticleDto> invalidArticles) {
        
        if (left > right) {
            return;
        }
        
        int mid = left + (right - left) / 2;
        Optional<LocalDateTime> midDate = extractDateSafely(articles.get(mid), sourceName);
        
        if (!midDate.isPresent()) {
            invalidArticles.add(articles.get(mid));
            // 날짜가 없으면 양쪽 모두 탐색
            searchInRecentArticles(articles, left, mid - 1, sourceName, cutoffTime, validArticles, invalidArticles);
            searchInRecentArticles(articles, mid + 1, right, sourceName, cutoffTime, validArticles, invalidArticles);
            return;
        }
        
        if (midDate.get().isAfter(cutoffTime)) {
            validArticles.add(articles.get(mid));
            // 유효하면 양쪽 모두 탐색 (더 많은 유효 기사가 있을 수 있음)
            searchInRecentArticles(articles, left, mid - 1, sourceName, cutoffTime, validArticles, invalidArticles);
            searchInRecentArticles(articles, mid + 1, right, sourceName, cutoffTime, validArticles, invalidArticles);
        } else {
            invalidArticles.add(articles.get(mid));
            // 무효하면 더 최근 쪽만 탐색
            searchInRecentArticles(articles, left, mid - 1, sourceName, cutoffTime, validArticles, invalidArticles);
        }
    }
    
    /**
     * 안전하게 날짜 추출 (예외 처리 포함)
     */
    private Optional<LocalDateTime> extractDateSafely(ArticleDto article, String sourceName) {
        try {
            return dateExtractor.extractPublishedDate(article.getUrl(), sourceName);
        } catch (Exception e) {
            System.err.println("날짜 추출 오류: " + article.getUrl() + " - " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 이진 탐색 결과를 담는 내부 클래스
     */
    private static class BinarySearchResult {
        final List<ArticleDto> validArticles;
        final List<ArticleDto> invalidArticles;
        
        BinarySearchResult(List<ArticleDto> validArticles, List<ArticleDto> invalidArticles) {
            this.validArticles = validArticles;
            this.invalidArticles = invalidArticles;
        }
    }
    
    /**
     * 서비스 종료 시 스레드 풀 정리
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}