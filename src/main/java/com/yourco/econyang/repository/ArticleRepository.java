package com.yourco.econyang.repository;

import com.yourco.econyang.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기사 엔티티 리포지토리
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * URL로 기사 조회
     */
    Optional<Article> findByUrl(String url);

    /**
     * 소스별 기사 조회
     */
    List<Article> findBySourceOrderByPublishedAtDesc(String source);

    /**
     * 발행 시간 범위로 기사 조회
     */
    List<Article> findByPublishedAtBetweenOrderByPublishedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 생성 시간 범위로 기사 조회 (수집 시간 기준)
     */
    List<Article> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 제목으로 기사 검색 (부분 일치)
     */
    List<Article> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(String titleKeyword);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 특정 소스의 최근 기사 조회
     */
    @Query(value = "SELECT a FROM Article a WHERE a.source = :source " +
           "ORDER BY COALESCE(a.publishedAt, a.createdAt) DESC")
    List<Article> findRecentBySource(@Param("source") String source);

    /**
     * 요약이 없는 기사 조회
     */
    @Query("SELECT a FROM Article a WHERE a.summaries IS EMPTY " +
           "ORDER BY COALESCE(a.publishedAt, a.createdAt) DESC")
    List<Article> findArticlesWithoutSummaries();

    /**
     * 특정 기간의 소스별 기사 수 통계
     */
    @Query("SELECT a.source, COUNT(a) FROM Article a " +
           "WHERE a.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY a.source ORDER BY COUNT(a) DESC")
    List<Object[]> countBySourceAndCreatedAtBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);

    /**
     * 오래된 기사 삭제 (배치 처리용)
     */
    @Modifying
    @Query("DELETE FROM Article a WHERE a.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Upsert 기능을 위한 URL 기반 업데이트
     */
    @Modifying
    @Query("UPDATE Article a SET a.title = :title, a.publishedAt = :publishedAt, " +
           "a.author = :author, a.rawExcerpt = :rawExcerpt " +
           "WHERE a.url = :url")
    int updateByUrl(@Param("url") String url, 
                   @Param("title") String title,
                   @Param("publishedAt") LocalDateTime publishedAt,
                   @Param("author") String author,
                   @Param("rawExcerpt") String rawExcerpt);

    /**
     * 본문이 추출된 기사만 조회
     */
    @Query("SELECT a FROM Article a WHERE a.rawExcerpt IS NOT NULL " +
           "AND LENGTH(TRIM(a.rawExcerpt)) > 0 " +
           "ORDER BY COALESCE(a.publishedAt, a.createdAt) DESC")
    List<Article> findArticlesWithContent();
}