package com.yourco.econdigest.domain;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 일일 경제뉴스 다이제스트 엔티티
 */
@Entity
@Table(name = "daily_digest")
public class DailyDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "digest_date", nullable = false, unique = true)
    private LocalDate digestDate;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body_markdown", nullable = false, columnDefinition = "TEXT")
    private String bodyMarkdown;

    @Column(name = "total_articles")
    private Integer totalArticles = 0;

    @Column(name = "total_summaries")
    private Integer totalSummaries = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "digest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DispatchLog> dispatchLogs = new ArrayList<>();

    protected DailyDigest() {
    }

    public DailyDigest(LocalDate digestDate, String title, String bodyMarkdown) {
        this.digestDate = digestDate;
        this.title = title;
        this.bodyMarkdown = bodyMarkdown;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public String getTitle() {
        return title;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public Integer getTotalArticles() {
        return totalArticles;
    }

    public Integer getTotalSummaries() {
        return totalSummaries;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<DispatchLog> getDispatchLogs() {
        return dispatchLogs;
    }

    // Setters
    public void setDigestDate(LocalDate digestDate) {
        this.digestDate = digestDate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBodyMarkdown(String bodyMarkdown) {
        this.bodyMarkdown = bodyMarkdown;
    }

    public void setTotalArticles(Integer totalArticles) {
        this.totalArticles = totalArticles;
    }

    public void setTotalSummaries(Integer totalSummaries) {
        this.totalSummaries = totalSummaries;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public void addDispatchLog(DispatchLog dispatchLog) {
        dispatchLogs.add(dispatchLog);
        dispatchLog.setDigest(this);
    }

    public void removeDispatchLog(DispatchLog dispatchLog) {
        dispatchLogs.remove(dispatchLog);
        dispatchLog.setDigest(null);
    }

    public void updateCounts(int articleCount, int summaryCount) {
        this.totalArticles = articleCount;
        this.totalSummaries = summaryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyDigest that = (DailyDigest) o;
        return Objects.equals(digestDate, that.digestDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digestDate);
    }

    @Override
    public String toString() {
        return "DailyDigest{" +
                "id=" + id +
                ", digestDate=" + digestDate +
                ", title='" + title + '\'' +
                ", totalArticles=" + totalArticles +
                ", totalSummaries=" + totalSummaries +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}