package com.yourco.econyang.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 기사 날짜 추출 결과 캐시 엔티티
 * 성공한 날짜 추출 결과를 저장해서 재사용
 */
@Entity
@Table(name = "article_date_cache", 
       indexes = {
           @Index(name = "idx_url_hash", columnList = "url_hash"),
           @Index(name = "idx_source_created", columnList = "source_name,created_at"),
           @Index(name = "idx_extraction_method", columnList = "extraction_method")
       })
public class ArticleDateCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "url_hash", length = 64, nullable = false, unique = true)
    private String urlHash; // URL의 SHA-256 해시 (인덱싱 효율성)
    
    @Column(name = "source_name", length = 100, nullable = false)
    private String sourceName;
    
    @Column(name = "extracted_date", nullable = false)
    private LocalDateTime extractedDate;
    
    @Column(name = "extraction_method", length = 50, nullable = false)
    private String extractionMethod; // "url", "meta", "regex", "pattern"
    
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore; // 0.0 ~ 1.0
    
    @Column(name = "extraction_details", columnDefinition = "TEXT")
    private String extractionDetails; // 추출 방법 상세 정보
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt; // 마지막 검증 시점
    
    @Column(name = "verification_count", nullable = false)
    private Integer verificationCount = 0; // 검증 횟수
    
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true; // 유효성 여부
    
    // Constructors
    public ArticleDateCache() {}
    
    public ArticleDateCache(String urlHash, String sourceName, LocalDateTime extractedDate,
                           String extractionMethod, Double confidenceScore, String extractionDetails) {
        this.urlHash = urlHash;
        this.sourceName = sourceName;
        this.extractedDate = extractedDate;
        this.extractionMethod = extractionMethod;
        this.confidenceScore = confidenceScore;
        this.extractionDetails = extractionDetails;
        this.createdAt = LocalDateTime.now();
        this.verificationCount = 1;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUrlHash() { return urlHash; }
    public void setUrlHash(String urlHash) { this.urlHash = urlHash; }
    
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    
    public LocalDateTime getExtractedDate() { return extractedDate; }
    public void setExtractedDate(LocalDateTime extractedDate) { this.extractedDate = extractedDate; }
    
    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public String getExtractionDetails() { return extractionDetails; }
    public void setExtractionDetails(String extractionDetails) { this.extractionDetails = extractionDetails; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    
    public Integer getVerificationCount() { return verificationCount; }
    public void setVerificationCount(Integer verificationCount) { this.verificationCount = verificationCount; }
    
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    
    // Utility methods
    public void incrementVerification() {
        this.verificationCount++;
        this.lastVerifiedAt = LocalDateTime.now();
    }
    
    public boolean isRecentlyCreated(int hours) {
        return createdAt.isAfter(LocalDateTime.now().minusHours(hours));
    }
    
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.7;
    }
    
    @Override
    public String toString() {
        return String.format("ArticleDateCache{id=%d, source=%s, method=%s, confidence=%.2f, date=%s}", 
                id, sourceName, extractionMethod, confidenceScore, extractedDate);
    }
}