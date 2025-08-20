package com.yourco.econyang.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 경제뉴스 요약 AI 응답을 위한 Structured Output DTO
 */
public class EconomicSummaryResponse {
    
    // 기사 요약
    private String summary;
    
    // 경제적 해석/분석
    private String analysis;
    
    // 중요도 점수 (1-10)
    @JsonProperty("importance_score")
    private Integer importanceScore;
    
    // 관련 경제 섹터/분야
    @JsonProperty("economic_sectors")
    private List<String> economicSectors;
    
    // 핵심 키워드
    private List<String> keywords;
    
    // 시장 영향도 (상승/하락/중립)
    @JsonProperty("market_impact")
    private String marketImpact;
    
    // 투자자 관심도 (높음/보통/낮음)
    @JsonProperty("investor_interest")
    private String investorInterest;
    
    // 요약의 신뢰도 점수 (1-10)
    @JsonProperty("confidence_score")
    private Integer confidenceScore;
    
    // 추가 컨텍스트나 배경 정보
    private String context;
    
    // 기본 생성자
    public EconomicSummaryResponse() {}
    
    // Getters and Setters
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
    
    public Integer getImportanceScore() {
        return importanceScore;
    }
    
    public void setImportanceScore(Integer importanceScore) {
        this.importanceScore = importanceScore;
    }
    
    public List<String> getEconomicSectors() {
        return economicSectors;
    }
    
    public void setEconomicSectors(List<String> economicSectors) {
        this.economicSectors = economicSectors;
    }
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
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
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    // 편의 메서드들 (JSON 직렬화에서 제외)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isHighImportance() {
        return importanceScore != null && importanceScore >= 7;
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 7;
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPositiveMarketImpact() {
        return "상승".equals(marketImpact) || "긍정".equals(marketImpact);
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isNegativeMarketImpact() {
        return "하락".equals(marketImpact) || "부정".equals(marketImpact);
    }
    
    @Override
    public String toString() {
        return "EconomicSummaryResponse{" +
                "summary='" + (summary != null && summary.length() > 50 ? 
                    summary.substring(0, 50) + "..." : summary) + '\'' +
                ", importanceScore=" + importanceScore +
                ", marketImpact='" + marketImpact + '\'' +
                ", confidenceScore=" + confidenceScore +
                '}';
    }
}