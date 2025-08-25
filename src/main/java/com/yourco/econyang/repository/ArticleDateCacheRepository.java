package com.yourco.econyang.repository;

import com.yourco.econyang.domain.ArticleDateCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기사 날짜 캐시 레포지토리
 */
@Repository
public interface ArticleDateCacheRepository extends JpaRepository<ArticleDateCache, Long> {
    
    /**
     * URL 해시로 캐시 엔트리 조회
     */
    Optional<ArticleDateCache> findByUrlHashAndIsValidTrue(String urlHash);
    
    /**
     * 특정 언론사의 캐시 엔트리들 조회 (최신순)
     */
    List<ArticleDateCache> findBySourceNameAndIsValidTrueOrderByCreatedAtDesc(String sourceName);
    
    /**
     * 특정 추출 방법의 성공 사례들 조회
     */
    List<ArticleDateCache> findByExtractionMethodAndIsValidTrueAndConfidenceScoreGreaterThan(
            String extractionMethod, Double minConfidence);
    
    /**
     * 특정 언론사의 최근 성공 사례들 조회 (학습용)
     */
    @Query("SELECT a FROM ArticleDateCache a WHERE a.sourceName = :sourceName " +
           "AND a.isValid = true AND a.confidenceScore >= :minConfidence " +
           "AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<ArticleDateCache> findRecentSuccessfulExtractions(@Param("sourceName") String sourceName,
                                                          @Param("minConfidence") Double minConfidence,
                                                          @Param("since") LocalDateTime since);
    
    /**
     * 언론사별 추출 방법 성공률 통계
     */
    @Query("SELECT a.extractionMethod, COUNT(a), AVG(a.confidenceScore) " +
           "FROM ArticleDateCache a WHERE a.sourceName = :sourceName " +
           "AND a.isValid = true AND a.createdAt >= :since " +
           "GROUP BY a.extractionMethod ORDER BY COUNT(a) DESC")
    List<Object[]> getExtractionMethodStats(@Param("sourceName") String sourceName,
                                           @Param("since") LocalDateTime since);
    
    /**
     * 오래된 캐시 엔트리 정리 (30일 이상)
     */
    @Modifying
    @Query("DELETE FROM ArticleDateCache a WHERE a.createdAt < :cutoffDate")
    int deleteOldCacheEntries(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 특정 언론사의 평균 신뢰도 계산
     */
    @Query("SELECT AVG(a.confidenceScore) FROM ArticleDateCache a " +
           "WHERE a.sourceName = :sourceName AND a.isValid = true " +
           "AND a.createdAt >= :since")
    Optional<Double> getAverageConfidenceScore(@Param("sourceName") String sourceName,
                                             @Param("since") LocalDateTime since);
    
    /**
     * 캐시 적중률 계산을 위한 총 조회 수 대비 적중 수
     */
    @Query("SELECT COUNT(a) FROM ArticleDateCache a WHERE a.sourceName = :sourceName " +
           "AND a.verificationCount > 1 AND a.createdAt >= :since")
    Long countCacheHits(@Param("sourceName") String sourceName, @Param("since") LocalDateTime since);
    
    /**
     * 신뢰도 낮은 엔트리들 무효화
     */
    @Modifying
    @Query("UPDATE ArticleDateCache a SET a.isValid = false " +
           "WHERE a.confidenceScore < :minConfidence OR a.verificationCount = 0")
    int invalidateLowConfidenceEntries(@Param("minConfidence") Double minConfidence);
}