package com.yourco.econdigest.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RSS 피드에서 수집한 기사 정보를 담는 DTO
 */
public class ArticleDto {
    
    private String source;        // RSS 소스 코드 (예: hankyung, maeil)
    private String url;           // 기사 URL (유니크 키)
    private String title;         // 기사 제목
    private String description;   // 기사 요약/설명
    private String author;        // 기사 작성자
    private LocalDateTime publishedAt;  // 발행 시간
    private LocalDateTime fetchedAt;    // 수집 시간
    private double sourceWeight;  // 소스 가중치
    
    // 본문 추출 관련 필드
    private String content;       // 추출된 본문 내용
    private LocalDateTime extractedAt;  // 본문 추출 시간
    private String extractError;  // 추출 실패 시 오류 메시지
    private boolean extractSuccess;  // 추출 성공 여부
    
    // 기본 생성자
    public ArticleDto() {
        this.fetchedAt = LocalDateTime.now();
    }
    
    // 전체 필드 생성자
    public ArticleDto(String source, String url, String title, String description, 
                     String author, LocalDateTime publishedAt, double sourceWeight) {
        this();
        this.source = source;
        this.url = url;
        this.title = title;
        this.description = description;
        this.author = author;
        this.publishedAt = publishedAt;
        this.sourceWeight = sourceWeight;
    }
    
    // Getters and Setters
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }
    
    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
    
    public double getSourceWeight() {
        return sourceWeight;
    }
    
    public void setSourceWeight(double sourceWeight) {
        this.sourceWeight = sourceWeight;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }
    
    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }
    
    public String getExtractError() {
        return extractError;
    }
    
    public void setExtractError(String extractError) {
        this.extractError = extractError;
    }
    
    public boolean isExtractSuccess() {
        return extractSuccess;
    }
    
    public void setExtractSuccess(boolean extractSuccess) {
        this.extractSuccess = extractSuccess;
    }
    
    /**
     * URL 기반 equals 및 hashCode (중복 제거용)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleDto that = (ArticleDto) o;
        return Objects.equals(url, that.url);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
    
    @Override
    public String toString() {
        return "ArticleDto{" +
                "source='" + source + '\'' +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publishedAt=" + publishedAt +
                ", fetchedAt=" + fetchedAt +
                ", sourceWeight=" + sourceWeight +
                '}';
    }
}