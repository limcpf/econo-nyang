package com.yourco.econyang.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 다이제스트 발송 로그 및 상태 추적 엔티티
 */
@Entity
@Table(name = "dispatch_log")
public class DispatchLog {

    public enum Status {
        SUCCESS, FAILED, PENDING, RETRY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digest_id", nullable = false)
    private DailyDigest digest;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "webhook_ref")
    private String webhookRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "response_snippet", columnDefinition = "TEXT")
    private String responseSnippet;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_count")
    private Integer attemptCount = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected DispatchLog() {
    }

    public DispatchLog(DailyDigest digest, String channel, Status status) {
        this.digest = digest;
        this.channel = channel;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public DispatchLog(DailyDigest digest, String channel, String webhookRef, Status status) {
        this(digest, channel, status);
        this.webhookRef = webhookRef;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attemptCount == null) {
            attemptCount = 1;
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public DailyDigest getDigest() {
        return digest;
    }

    public String getChannel() {
        return channel;
    }

    public String getWebhookRef() {
        return webhookRef;
    }

    public Status getStatus() {
        return status;
    }

    public String getResponseSnippet() {
        return responseSnippet;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setDigest(DailyDigest digest) {
        this.digest = digest;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setWebhookRef(String webhookRef) {
        this.webhookRef = webhookRef;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setResponseSnippet(String responseSnippet) {
        this.responseSnippet = responseSnippet;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public void markAsSuccess(String responseSnippet) {
        this.status = Status.SUCCESS;
        this.responseSnippet = responseSnippet;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markForRetry(String errorMessage) {
        this.status = Status.RETRY;
        this.errorMessage = errorMessage;
        this.attemptCount = (this.attemptCount == null) ? 2 : this.attemptCount + 1;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean needsRetry() {
        return status == Status.RETRY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DispatchLog that = (DispatchLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DispatchLog{" +
                "id=" + id +
                ", channel='" + channel + '\'' +
                ", status=" + status +
                ", attemptCount=" + attemptCount +
                ", createdAt=" + createdAt +
                '}';
    }
}