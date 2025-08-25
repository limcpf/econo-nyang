package com.yourco.econyang.service;

import com.yourco.econyang.domain.ArticleDateCache;
import com.yourco.econyang.repository.ArticleDateCacheRepository;
import com.yourco.econyang.strategy.SmartDateFilterStrategy.DateEstimationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기사 날짜 추출 결과 캐싱 서비스
 * 성공한 날짜 추출 결과를 저장하고 재사용하여 성능 개선
 */
@Service
@Transactional
public class ArticleDateCacheService {
    
    private final ArticleDateCacheRepository cacheRepository;
    
    // 인메모리 캐시 (자주 사용되는 결과 임시 저장)
    private final ConcurrentHashMap<String, ArticleDateCache> inMemoryCache = new ConcurrentHashMap<>();
    private static final int MAX_MEMORY_CACHE_SIZE = 1000;
    
    @Autowired
    public ArticleDateCacheService(ArticleDateCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }
    
    /**
     * URL에 대한 캐시된 날짜 조회
     */
    public Optional<DateEstimationResult> getCachedDate(String url, String sourceName) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String urlHash = hashUrl(url);
        
        // 1. 인메모리 캐시 확인
        ArticleDateCache memoryCache = inMemoryCache.get(urlHash);
        if (memoryCache != null && memoryCache.getIsValid()) {
            memoryCache.incrementVerification();
            return Optional.of(toCacheResult(memoryCache));
        }
        
        // 2. DB 캐시 확인
        Optional<ArticleDateCache> dbCache = cacheRepository.findByUrlHashAndIsValidTrue(urlHash);
        if (dbCache.isPresent()) {
            ArticleDateCache cache = dbCache.get();
            cache.incrementVerification();
            
            // 인메모리 캐시에 저장 (크기 제한)
            if (inMemoryCache.size() < MAX_MEMORY_CACHE_SIZE) {
                inMemoryCache.put(urlHash, cache);
            }
            
            System.out.println(String.format("캐시 적중: %s (%s, 신뢰도: %.2f)", 
                    sourceName, cache.getExtractionMethod(), cache.getConfidenceScore()));
            
            return Optional.of(toCacheResult(cache));
        }
        
        return Optional.empty();
    }
    
    /**
     * 날짜 추출 결과를 캐시에 저장
     */
    public void saveDateExtractionResult(String url, String sourceName, DateEstimationResult result) {
        if (url == null || sourceName == null || result == null || !result.isValid()) {
            return;
        }
        
        String urlHash = hashUrl(url);
        
        // 이미 존재하는지 확인
        Optional<ArticleDateCache> existing = cacheRepository.findByUrlHashAndIsValidTrue(urlHash);
        if (existing.isPresent()) {
            // 기존 엔트리 업데이트
            ArticleDateCache cache = existing.get();
            
            // 더 높은 신뢰도면 업데이트
            if (result.getConfidenceScore() > cache.getConfidenceScore()) {
                cache.setExtractedDate(result.getEstimatedDate().get());
                cache.setExtractionMethod(result.getExtractionMethod());
                cache.setConfidenceScore(result.getConfidenceScore());
                cache.setExtractionDetails(result.getDetails());
                cache.incrementVerification();
                
                // 인메모리 캐시도 업데이트
                inMemoryCache.put(urlHash, cache);
                
                System.out.println(String.format("캐시 업데이트: %s (%s, 신뢰도: %.2f → %.2f)", 
                        sourceName, result.getExtractionMethod(), 
                        existing.get().getConfidenceScore(), result.getConfidenceScore()));
            }
            return;
        }
        
        // 새로운 캐시 엔트리 생성
        ArticleDateCache newCache = new ArticleDateCache(
                urlHash,
                sourceName,
                result.getEstimatedDate().get(),
                result.getExtractionMethod(),
                result.getConfidenceScore(),
                result.getDetails()
        );
        
        ArticleDateCache saved = cacheRepository.save(newCache);
        
        // 인메모리 캐시에도 저장
        if (inMemoryCache.size() < MAX_MEMORY_CACHE_SIZE) {
            inMemoryCache.put(urlHash, saved);
        }
        
        System.out.println(String.format("캐시 저장: %s (%s, 신뢰도: %.2f)", 
                sourceName, result.getExtractionMethod(), result.getConfidenceScore()));
    }
    
    /**
     * 특정 언론사의 학습 데이터 조회
     */
    public List<ArticleDateCache> getLearningData(String sourceName, int recentDays, double minConfidence) {
        LocalDateTime since = LocalDateTime.now().minusDays(recentDays);
        return cacheRepository.findRecentSuccessfulExtractions(sourceName, minConfidence, since);
    }
    
    /**
     * 언론사별 추출 방법 성공률 통계
     */
    public void printExtractionStats(String sourceName) {
        LocalDateTime since = LocalDateTime.now().minusDays(7); // 최근 7일
        List<Object[]> stats = cacheRepository.getExtractionMethodStats(sourceName, since);
        
        System.out.println(String.format("=== %s 추출 방법 통계 (최근 7일) ===", sourceName));
        for (Object[] stat : stats) {
            String method = (String) stat[0];
            Long count = (Long) stat[1];
            Double avgConfidence = (Double) stat[2];
            
            System.out.println(String.format("- %s: %d회 (평균 신뢰도: %.2f)", 
                    method, count, avgConfidence));
        }
    }
    
    /**
     * 캐시 정리 (오래된 엔트리 삭제)
     */
    @Transactional
    public int cleanupOldCache() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = cacheRepository.deleteOldCacheEntries(cutoffDate);
        
        // 인메모리 캐시도 정리
        inMemoryCache.clear();
        
        System.out.println(String.format("캐시 정리 완료: %d개 엔트리 삭제", deleted));
        return deleted;
    }
    
    /**
     * 신뢰도 낮은 엔트리들 무효화
     */
    @Transactional
    public int invalidateLowConfidenceCache(double minConfidence) {
        int invalidated = cacheRepository.invalidateLowConfidenceEntries(minConfidence);
        
        // 인메모리 캐시에서도 제거
        inMemoryCache.entrySet().removeIf(entry -> 
                entry.getValue().getConfidenceScore() < minConfidence);
        
        System.out.println(String.format("낮은 신뢰도 캐시 무효화: %d개 엔트리", invalidated));
        return invalidated;
    }
    
    /**
     * 캐시 성능 통계 출력
     */
    public void printCacheStats() {
        System.out.println("=== 캐시 성능 통계 ===");
        System.out.println("인메모리 캐시 크기: " + inMemoryCache.size());
        
        // 언론사별 통계
        String[] sources = {"Financial Times", "Bloomberg Economics", "MarketWatch", "매일경제"};
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        
        for (String source : sources) {
            Long hits = cacheRepository.countCacheHits(source, since);
            Optional<Double> avgConfidence = cacheRepository.getAverageConfidenceScore(source, since);
            
            System.out.println(String.format("- %s: 캐시 적중 %d회, 평균 신뢰도 %.2f", 
                    source, hits, avgConfidence.orElse(0.0)));
        }
    }
    
    // Private methods
    
    /**
     * URL을 SHA-256으로 해싱
     */
    private String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
    
    /**
     * ArticleDateCache를 DateEstimationResult로 변환
     */
    private DateEstimationResult toCacheResult(ArticleDateCache cache) {
        return new DateEstimationResult(
                Optional.of(cache.getExtractedDate()),
                cache.getConfidenceScore(),
                cache.getExtractionMethod() + "_cached",
                "캐시에서 조회: " + cache.getExtractionDetails()
        );
    }
}