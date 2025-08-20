package com.yourco.econyang.dto;

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
    
    // AI 요약 관련 필드
    private String aiSummary;     // AI 생성 요약
    private String aiAnalysis;    // AI 생성 경제 분석
    private Integer importanceScore;  // 중요도 점수 (1-10)
    private String marketImpact;  // 시장 영향도
    private String investorInterest;  // 투자자 관심도
    private Integer confidenceScore;  // 신뢰도 점수 (1-10)
    private LocalDateTime summarizedAt;  // AI 요약 생성 시간
    private String summarizeError;    // 요약 실패 시 오류 메시지
    private boolean summarizeSuccess; // 요약 성공 여부
    
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
    
    // AI 요약 필드들의 Getters and Setters
    public String getAiSummary() {
        return aiSummary;
    }
    
    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
    
    public String getAiAnalysis() {
        return aiAnalysis;
    }
    
    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
    
    public Integer getImportanceScore() {
        return importanceScore;
    }
    
    public void setImportanceScore(Integer importanceScore) {
        this.importanceScore = importanceScore;
    }
    
    public String getMarketImpact() {
        return marketImpact;
    }
    
    public void setMarketImpact(String marketImpact) {
        this.marketImpact = marketImpact;
    }
    
    public String getInvestorInterest() {
        return investorInterest;
    }
    
    public void setInvestorInterest(String investorInterest) {
        this.investorInterest = investorInterest;
    }
    
    public Integer getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public LocalDateTime getSummarizedAt() {
        return summarizedAt;
    }
    
    public void setSummarizedAt(LocalDateTime summarizedAt) {
        this.summarizedAt = summarizedAt;
    }
    
    public String getSummarizeError() {
        return summarizeError;
    }
    
    public void setSummarizeError(String summarizeError) {
        this.summarizeError = summarizeError;
    }
    
    public boolean isSummarizeSuccess() {
        return summarizeSuccess;
    }
    
    public void setSummarizeSuccess(boolean summarizeSuccess) {
        this.summarizeSuccess = summarizeSuccess;
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