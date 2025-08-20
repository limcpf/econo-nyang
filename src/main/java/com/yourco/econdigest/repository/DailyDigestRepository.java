package com.yourco.econdigest.repository;

import com.yourco.econdigest.domain.DailyDigest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 일일 다이제스트 엔티티 리포지토리
 */
@Repository
public interface DailyDigestRepository extends JpaRepository<DailyDigest, Long> {

    /**
     * 날짜로 다이제스트 조회
     */
    Optional<DailyDigest> findByDigestDate(LocalDate digestDate);

    /**
     * 날짜 범위로 다이제스트 조회 (최신순)
     */
    List<DailyDigest> findByDigestDateBetweenOrderByDigestDateDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * 최근 다이제스트 조회
     */
    List<DailyDigest> findTop10ByOrderByDigestDateDesc();

    /**
     * 특정 날짜 다이제스트 존재 여부 확인
     */
    boolean existsByDigestDate(LocalDate digestDate);

    /**
     * 생성 시간 기준 다이제스트 조회
     */
    List<DailyDigest> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 업데이트 시간 기준 다이제스트 조회
     */
    List<DailyDigest> findByUpdatedAtBetweenOrderByUpdatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 기사 수가 특정 값 이상인 다이제스트 조회
     */
    List<DailyDigest> findByTotalArticlesGreaterThanEqualOrderByDigestDateDesc(
            Integer minArticles);

    /**
     * 요약 수가 특정 값 이상인 다이제스트 조회
     */
    List<DailyDigest> findByTotalSummariesGreaterThanEqualOrderByDigestDateDesc(
            Integer minSummaries);

    /**
     * 제목으로 다이제스트 검색
     */
    List<DailyDigest> findByTitleContainingIgnoreCaseOrderByDigestDateDesc(String titleKeyword);

    /**
     * 월별 다이제스트 조회
     */
    @Query("SELECT d FROM DailyDigest d " +
           "WHERE YEAR(d.digestDate) = :year AND MONTH(d.digestDate) = :month " +
           "ORDER BY d.digestDate DESC")
    List<DailyDigest> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 연도별 다이제스트 조회
     */
    @Query("SELECT d FROM DailyDigest d " +
           "WHERE YEAR(d.digestDate) = :year " +
           "ORDER BY d.digestDate DESC")
    List<DailyDigest> findByYear(@Param("year") int year);

    /**
     * 다이제스트 통계 조회 (총 기사 수, 총 요약 수)
     */
    @Query("SELECT SUM(d.totalArticles), SUM(d.totalSummaries), COUNT(d) " +
           "FROM DailyDigest d " +
           "WHERE d.digestDate BETWEEN :startDate AND :endDate")
    Object[] getStatistics(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate);

    /**
     * 월별 통계
     */
    @Query("SELECT YEAR(d.digestDate), MONTH(d.digestDate), " +
           "COUNT(d), SUM(d.totalArticles), SUM(d.totalSummaries) " +
           "FROM DailyDigest d " +
           "GROUP BY YEAR(d.digestDate), MONTH(d.digestDate) " +
           "ORDER BY YEAR(d.digestDate) DESC, MONTH(d.digestDate) DESC")
    List<Object[]> getMonthlyStatistics();

    /**
     * 다이제스트 카운트 업데이트
     */
    @Modifying
    @Query("UPDATE DailyDigest d SET d.totalArticles = :articleCount, " +
           "d.totalSummaries = :summaryCount WHERE d.id = :id")
    int updateCounts(@Param("id") Long id, 
                    @Param("articleCount") Integer articleCount,
                    @Param("summaryCount") Integer summaryCount);

    /**
     * 오래된 다이제스트 삭제
     */
    @Modifying
    @Query("DELETE FROM DailyDigest d WHERE d.digestDate < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 발송되지 않은 다이제스트 조회 (발송 로그가 없는 경우)
     */
    @Query("SELECT d FROM DailyDigest d " +
           "WHERE d.dispatchLogs IS EMPTY " +
           "ORDER BY d.digestDate DESC")
    List<DailyDigest> findUnsentDigests();

    /**
     * 발송 실패한 다이제스트 조회
     */
    @Query("SELECT DISTINCT d FROM DailyDigest d " +
           "JOIN d.dispatchLogs dl " +
           "WHERE dl.status = 'FAILED' " +
           "ORDER BY d.digestDate DESC")
    List<DailyDigest> findFailedDispatches();
}