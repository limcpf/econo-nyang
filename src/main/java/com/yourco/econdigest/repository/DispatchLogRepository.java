package com.yourco.econdigest.repository;

import com.yourco.econdigest.domain.DailyDigest;
import com.yourco.econdigest.domain.DispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 발송 로그 엔티티 리포지토리
 */
@Repository
public interface DispatchLogRepository extends JpaRepository<DispatchLog, Long> {

    /**
     * 특정 다이제스트의 발송 로그 조회
     */
    List<DispatchLog> findByDigestOrderByCreatedAtDesc(DailyDigest digest);

    /**
     * 특정 다이제스트와 채널의 발송 로그 조회
     */
    List<DispatchLog> findByDigestAndChannelOrderByCreatedAtDesc(DailyDigest digest, String channel);

    /**
     * 상태별 발송 로그 조회
     */
    List<DispatchLog> findByStatusOrderByCreatedAtDesc(DispatchLog.Status status);

    /**
     * 채널별 발송 로그 조회
     */
    List<DispatchLog> findByChannelOrderByCreatedAtDesc(String channel);

    /**
     * 실패한 발송 로그 조회 (재시도 대상)
     */
    List<DispatchLog> findByStatusInOrderByCreatedAtDesc(List<DispatchLog.Status> statuses);

    /**
     * 특정 기간의 발송 로그 조회
     */
    List<DispatchLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 최근 발송 로그 조회
     */
    List<DispatchLog> findTop50ByOrderByCreatedAtDesc();

    /**
     * 특정 다이제스트의 성공한 발송 조회
     */
    @Query("SELECT dl FROM DispatchLog dl " +
           "WHERE dl.digest = :digest AND dl.status = 'SUCCESS' " +
           "ORDER BY dl.createdAt DESC")
    List<DispatchLog> findSuccessfulDispatches(@Param("digest") DailyDigest digest);

    /**
     * 특정 다이제스트의 실패한 발송 조회
     */
    @Query("SELECT dl FROM DispatchLog dl " +
           "WHERE dl.digest = :digest AND dl.status = 'FAILED' " +
           "ORDER BY dl.createdAt DESC")
    List<DispatchLog> findFailedDispatches(@Param("digest") DailyDigest digest);

    /**
     * 재시도가 필요한 발송 로그 조회
     */
    @Query("SELECT dl FROM DispatchLog dl " +
           "WHERE dl.status = 'RETRY' AND dl.attemptCount < :maxAttempts " +
           "ORDER BY dl.createdAt ASC")
    List<DispatchLog> findRetryableDispatches(@Param("maxAttempts") int maxAttempts);

    /**
     * 채널별 발송 통계
     */
    @Query("SELECT dl.channel, dl.status, COUNT(dl) " +
           "FROM DispatchLog dl " +
           "WHERE dl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY dl.channel, dl.status " +
           "ORDER BY dl.channel, dl.status")
    List<Object[]> getChannelStatistics(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 일별 발송 통계
     */
    @Query("SELECT DATE(dl.createdAt), dl.status, COUNT(dl) " +
           "FROM DispatchLog dl " +
           "WHERE dl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(dl.createdAt), dl.status " +
           "ORDER BY DATE(dl.createdAt) DESC, dl.status")
    List<Object[]> getDailyStatistics(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 특정 다이제스트와 채널의 최근 발송 상태 조회
     */
    Optional<DispatchLog> findTopByDigestAndChannelOrderByCreatedAtDesc(
            DailyDigest digest, String channel);

    /**
     * 발송 상태 업데이트
     */
    @Modifying
    @Query("UPDATE DispatchLog dl SET dl.status = :status, " +
           "dl.responseSnippet = :responseSnippet, dl.errorMessage = :errorMessage " +
           "WHERE dl.id = :id")
    int updateStatus(@Param("id") Long id, 
                    @Param("status") DispatchLog.Status status,
                    @Param("responseSnippet") String responseSnippet,
                    @Param("errorMessage") String errorMessage);

    /**
     * 재시도 횟수 증가
     */
    @Modifying
    @Query("UPDATE DispatchLog dl SET dl.attemptCount = dl.attemptCount + 1, " +
           "dl.status = :status, dl.errorMessage = :errorMessage " +
           "WHERE dl.id = :id")
    int incrementAttemptCount(@Param("id") Long id, 
                             @Param("status") DispatchLog.Status status,
                             @Param("errorMessage") String errorMessage);

    /**
     * 오래된 발송 로그 삭제
     */
    @Modifying
    @Query("DELETE FROM DispatchLog dl WHERE dl.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 성공률 계산 (특정 기간)
     */
    @Query("SELECT " +
           "SUM(CASE WHEN dl.status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(dl) " +
           "FROM DispatchLog dl " +
           "WHERE dl.createdAt BETWEEN :startTime AND :endTime")
    Double getSuccessRate(@Param("startTime") LocalDateTime startTime, 
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 채널별 성공률 계산
     */
    @Query("SELECT dl.channel, " +
           "SUM(CASE WHEN dl.status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(dl) " +
           "FROM DispatchLog dl " +
           "WHERE dl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY dl.channel " +
           "ORDER BY dl.channel")
    List<Object[]> getSuccessRateByChannel(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
}