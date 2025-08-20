package com.yourco.econdigest.repository;

import com.yourco.econdigest.domain.Article;
import com.yourco.econdigest.domain.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 요약 엔티티 리포지토리
 */
@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    /**
     * 특정 기사와 모델의 요약 조회
     */
    Optional<Summary> findByArticleAndModel(Article article, String model);

    /**
     * 특정 기사의 모든 요약 조회
     */
    List<Summary> findByArticleOrderByCreatedAtDesc(Article article);

    /**
     * 특정 모델의 요약들 조회
     */
    List<Summary> findByModelOrderByCreatedAtDesc(String model);

    /**
     * 점수 범위로 요약 조회
     */
    List<Summary> findByScoreBetweenOrderByScoreDesc(BigDecimal minScore, BigDecimal maxScore);

    /**
     * 높은 점수순 요약 조회
     */
    List<Summary> findTop10ByOrderByScoreDesc();

    /**
     * 특정 기간의 요약 조회
     */
    List<Summary> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 기사 URL과 모델로 요약 존재 여부 확인
     */
    @Query("SELECT COUNT(s) > 0 FROM Summary s " +
           "WHERE s.article.url = :url AND s.model = :model")
    boolean existsByArticleUrlAndModel(@Param("url") String url, @Param("model") String model);

    /**
     * 특정 점수 이상의 최근 요약 조회
     */
    @Query("SELECT s FROM Summary s " +
           "WHERE s.score >= :minScore " +
           "ORDER BY s.createdAt DESC")
    List<Summary> findRecentByMinScore(@Param("minScore") BigDecimal minScore);

    /**
     * 특정 기간 동안의 모델별 요약 수 통계
     */
    @Query("SELECT s.model, COUNT(s), AVG(s.score) FROM Summary s " +
           "WHERE s.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY s.model ORDER BY COUNT(s) DESC")
    List<Object[]> getModelStatistics(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 다이제스트용 고품질 요약 조회 (점수 기준)
     */
    @Query("SELECT s FROM Summary s " +
           "WHERE s.score >= :minScore " +
           "AND s.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY s.score DESC, s.createdAt DESC")
    List<Summary> findForDigest(@Param("minScore") BigDecimal minScore,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 특정 기사의 최고 점수 요약 조회
     */
    @Query("SELECT s FROM Summary s " +
           "WHERE s.article = :article " +
           "ORDER BY s.score DESC, s.createdAt DESC")
    List<Summary> findBestByArticle(@Param("article") Article article);

    /**
     * 요약 점수 업데이트
     */
    @Modifying
    @Query("UPDATE Summary s SET s.score = :score WHERE s.id = :id")
    int updateScore(@Param("id") Long id, @Param("score") BigDecimal score);

    /**
     * 오래된 요약 삭제 (데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM Summary s WHERE s.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 키워드가 포함된 요약 검색
     */
    @Query("SELECT s FROM Summary s " +
           "WHERE LOWER(s.summaryText) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.whyItMatters) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY s.score DESC, s.createdAt DESC")
    List<Summary> findByKeyword(@Param("keyword") String keyword);

    /**
     * 중복 요약 조회 (같은 기사, 같은 모델)
     */
    @Query("SELECT s1 FROM Summary s1, Summary s2 " +
           "WHERE s1.article = s2.article AND s1.model = s2.model " +
           "AND s1.id < s2.id")
    List<Summary> findDuplicates();
}