package com.yourco.econyang.service;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.repository.SummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 요약 생성 및 Summary 엔티티 관리 서비스
 */
@Service
public class SummaryService {
    
    @Autowired
    private OpenAiClient openAiClient;
    
    @Autowired
    private SummaryRepository summaryRepository;
    
    @Autowired
    private ApiUsageMonitoringService usageMonitoringService;
    
    @Value("${app.openai.modelMain:gpt-4o}")
    private String defaultModel;
    
    @Value("${app.ai.summary.enabled:true}")
    private boolean aiSummaryEnabled;
    
    @Value("${app.ai.summary.minContentLength:100}")
    private int minContentLength;
    
    @Value("${app.ai.summary.maxRetries:2}")
    private int maxRetries;
    
    /**
     * 기사에 대한 AI 요약 생성
     */
    public Summary generateSummary(Article article) {
        return generateSummary(article, defaultModel, true);
    }
    
    /**
     * 기사에 대한 AI 요약 생성 (상세 옵션)
     */
    public Summary generateSummary(Article article, String model, boolean saveToDb) {
        if (article == null) {
            throw new IllegalArgumentException("Article cannot be null");
        }
        
        // 기존 요약이 있는지 확인
        Optional<Summary> existingSummary = summaryRepository.findByArticleAndModel(article, model);
        if (existingSummary.isPresent()) {
            System.out.println("기존 요약 발견: " + article.getUrl());
            return existingSummary.get();
        }
        
        // 본문과 RSS 메타데이터 확인
        String content = article.getContent();
        boolean hasContent = content != null && content.length() >= minContentLength;
        boolean hasRssData = (article.getTitle() != null && !article.getTitle().trim().isEmpty()) ||
                           (article.getRawExcerpt() != null && !article.getRawExcerpt().trim().isEmpty());
        
        // 본문도 RSS 데이터도 충분하지 않은 경우만 실패
        if (!hasContent && !hasRssData) {
            return createFallbackSummary(article, model, "본문과 RSS 메타데이터 모두 불충분합니다");
        }
        
        // 본문이 없으면 RSS 메타데이터로 AI 추론 요약 시도
        if (!hasContent && hasRssData) {
            System.out.println("본문 없음, RSS 메타데이터로 AI 추론 요약 생성: " + article.getUrl());
            return generateIntelligentInferenceSummary(article, model);
        }
        
        // AI 요약이 비활성화된 경우
        if (!aiSummaryEnabled) {
            return createFallbackSummary(article, model, "AI 요약이 비활성화되었습니다");
        }
        
        Summary summary = null;
        Exception lastException = null;
        
        // 재시도 로직
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                // 토큰 사용량 추정
                int estimatedInputTokens = openAiClient.estimateTokens(article.getTitle() + " " + content);
                int estimatedOutputTokens = 300; // 평균 출력 토큰 추정
                
                // OpenAI API 호출
                EconomicSummaryResponse aiResponse = openAiClient.generateEconomicSummary(
                    article.getTitle(), content);
                
                // Summary 엔티티 생성
                summary = convertToSummary(article, model, aiResponse);
                
                // 사용량 모니터링 기록
                usageMonitoringService.recordRequest(model, estimatedInputTokens, estimatedOutputTokens, true, null);
                
                System.out.println("AI 요약 생성 성공: " + article.getUrl() + 
                                 " (시도: " + attempt + "/" + (maxRetries + 1) + ")");
                break;
                
            } catch (Exception e) {
                // 실패 기록
                usageMonitoringService.recordRequest(model, 0, 0, false, e.getMessage());
                lastException = e;
                System.err.println("AI 요약 생성 실패 (시도 " + attempt + "/" + (maxRetries + 1) + "): " + 
                                 article.getUrl() + " - " + e.getMessage());
                
                if (attempt < maxRetries + 1) {
                    try {
                        Thread.sleep(1000 * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 모든 시도 실패 시 폴백 요약 생성
        if (summary == null) {
            String errorMsg = lastException != null ? lastException.getMessage() : "알 수 없는 오류";
            summary = createFallbackSummary(article, model, errorMsg);
        }
        
        // DB 저장
        if (saveToDb) {
            try {
                summary = summaryRepository.save(summary);
                System.out.println("Summary DB 저장 완료: " + summary.getId());
            } catch (Exception e) {
                System.err.println("Summary DB 저장 실패: " + e.getMessage());
            }
        }
        
        return summary;
    }
    
    /**
     * 여러 기사에 대한 배치 요약 생성
     */
    public List<Summary> generateSummaries(List<Article> articles) {
        return generateSummaries(articles, defaultModel, true);
    }
    
    /**
     * 여러 기사에 대한 배치 요약 생성 (상세 옵션)
     */
    public List<Summary> generateSummaries(List<Article> articles, String model, boolean saveToDb) {
        if (articles == null || articles.isEmpty()) {
            throw new IllegalArgumentException("Articles list cannot be null or empty");
        }
        
        System.out.println("배치 AI 요약 생성 시작: " + articles.size() + "개 기사");
        long startTime = System.currentTimeMillis();
        
        List<Summary> summaries = articles.stream()
                .map(article -> generateSummary(article, model, saveToDb))
                .collect(java.util.stream.Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        // 결과 통계
        long successCount = summaries.stream()
                .mapToLong(summary -> summary.getScore() != null && 
                    summary.getScore().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .sum();
        
        System.out.println("배치 AI 요약 완료: " + successCount + "/" + summaries.size() + 
                         " 성공, " + String.format("%.2f", processingTime) + "초 소요");
        
        return summaries;
    }
    
    /**
     * 기사별 최고 점수 요약 조회
     */
    public Optional<Summary> getBestSummaryForArticle(Article article) {
        List<Summary> summaries = summaryRepository.findBestByArticle(article);
        return summaries.isEmpty() ? Optional.empty() : Optional.of(summaries.get(0));
    }
    
    /**
     * 점수 범위로 요약 조회
     */
    public List<Summary> getSummariesByScoreRange(double minScore, double maxScore) {
        return summaryRepository.findByScoreBetweenOrderByScoreDesc(
            BigDecimal.valueOf(minScore), BigDecimal.valueOf(maxScore));
    }
    
    /**
     * OpenAI API 사용 가능 여부 확인
     */
    public boolean isAiSummaryAvailable() {
        return aiSummaryEnabled && openAiClient.isApiAvailable();
    }
    
    /**
     * API 사용량 통계 출력
     */
    public void printApiUsageStats() {
        usageMonitoringService.printStats();
    }
    
    /**
     * API 사용량 통계 조회
     */
    public ApiUsageMonitoringService.UsageStats getApiUsageStats() {
        return usageMonitoringService.getCurrentStats();
    }
    
    // === Private Methods ===
    
    /**
     * RSS 메타데이터로 AI 추론 요약 생성
     */
    private Summary generateIntelligentInferenceSummary(Article article, String model) {
        try {
            // 전문가 수준 추론 프롬프트 생성
            String inferencePrompt = createExpertInferencePrompt(article);
            
            // OpenAI로 추론 요약 생성 (간단한 응답 사용)
            String inferenceResult = openAiClient.generateSimpleSummary(inferencePrompt, 500);
            
            // 추론 결과를 구조화된 요약으로 변환
            return parseInferenceResult(article, model, inferenceResult);
            
        } catch (Exception e) {
            System.err.println("AI 추론 요약 실패, 기본 폴백으로 전환: " + article.getUrl() + " - " + e.getMessage());
            return createFallbackSummary(article, model, "AI 추론 실패: " + e.getMessage());
        }
    }
    
    /**
     * 전문가 수준 추론을 위한 프롬프트 생성
     */
    private String createExpertInferencePrompt(Article article) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 20년 경력의 경제뉴스 전문 애널리스트입니다. ")
              .append("다음 정보만으로 투자자에게 유용한 상세 분석을 작성하세요.\n\n");
        
        // 기사 정보
        prompt.append("=== 기사 정보 ===\n");
        prompt.append("제목: ").append(article.getTitle()).append("\n");
        prompt.append("출처: ").append(article.getSource()).append("\n");
        
        if (article.getRawExcerpt() != null && !article.getRawExcerpt().trim().isEmpty()) {
            prompt.append("RSS 요약: ").append(article.getRawExcerpt()).append("\n");
        }
        
        if (article.getPublishedAt() != null) {
            prompt.append("발행일: ").append(article.getPublishedAt()).append("\n");
        }
        
        // 출처별 컨텍스트 추가
        prompt.append("\n=== 출처 특성 ===\n");
        if (article.getSource().toLowerCase().contains("investing")) {
            prompt.append("- Investing.com: 투자분석 전문지, SWOT분석/재무분석/투자의견 중심\n");
            prompt.append("- 주요 독자층: 개인투자자, 기관투자자\n");
        } else if (article.getSource().toLowerCase().contains("bloomberg")) {
            prompt.append("- Bloomberg: 글로벌 금융정보 전문지, 거시경제/시장동향 중심\n");
        } else if (article.getSource().toLowerCase().contains("bbc")) {
            prompt.append("- BBC Business: 글로벌 관점의 경제뉴스, 정책/트렌드 분석\n");
        }
        
        // 분석 지침
        prompt.append("\n=== 분석 지침 ===\n");
        prompt.append("1. 제목에서 핵심 키워드 추출 및 경제적 의미 분석\n");
        prompt.append("2. 출처 특성을 고려한 기사 내용 추론\n");
        prompt.append("3. 해당 분야의 일반적 트렌드 및 시장 맥락 반영\n");
        prompt.append("4. 투자자 관점에서의 중요도 및 영향 평가\n");
        prompt.append("5. 1-10점 척도로 중요도 점수 부여\n\n");
        
        prompt.append("응답 형식:\n");
        prompt.append("**요약**: (2-3문장으로 핵심 내용 설명)\n");
        prompt.append("**분석**: (상세 맥락 분석 및 추론)\n");
        prompt.append("**투자자 관심**: (투자 관점에서의 의미)\n");
        prompt.append("**중요도**: (1-10점)\n");
        prompt.append("**키워드**: (관련 키워드 3-5개, 쉼표로 구분)");
        
        return prompt.toString();
    }
    
    /**
     * AI 추론 결과를 Summary 객체로 파싱
     */
    private Summary parseInferenceResult(Article article, String model, String inferenceResult) {
        try {
            // 기본값 설정
            String summaryText = "";
            String analysisText = "";
            int importanceScore = 5;
            String keywords = "경제뉴스";
            
            // 추론 결과 파싱
            String[] sections = inferenceResult.split("\\*\\*");
            for (int i = 0; i < sections.length - 1; i++) {
                String section = sections[i].trim();
                String nextSection = sections[i + 1].trim();
                
                if (section.endsWith("요약")) {
                    summaryText = extractContent(nextSection);
                } else if (section.endsWith("분석")) {
                    analysisText = extractContent(nextSection);
                } else if (section.endsWith("중요도")) {
                    String scoreStr = extractContent(nextSection);
                    try {
                        // "8/10" 또는 "8점" 형태에서 숫자 추출
                        scoreStr = scoreStr.replaceAll("[^0-9]", "");
                        if (!scoreStr.isEmpty()) {
                            importanceScore = Math.min(10, Math.max(1, Integer.parseInt(scoreStr)));
                        }
                    } catch (NumberFormatException e) {
                        // 파싱 실패 시 기본값 사용
                    }
                } else if (section.endsWith("키워드")) {
                    keywords = extractContent(nextSection);
                }
            }
            
            // Summary 객체 생성
            Summary summary = new Summary(article, model, summaryText, analysisText);
            summary.setScore(BigDecimal.valueOf(importanceScore));
            
            // 키워드 설정
            String[] keywordArray = keywords.split("[,，]");
            List<String> keywordList = new java.util.ArrayList<>();
            for (String keyword : keywordArray) {
                String cleanKeyword = keyword.trim();
                if (!cleanKeyword.isEmpty()) {
                    keywordList.add(cleanKeyword);
                }
            }
            if (!keywordList.isEmpty()) {
                summary.setBulletsList(keywordList);
            } else {
                summary.setBulletsList(java.util.Arrays.asList("경제뉴스"));
            }
            
            // 추가 메타데이터
            List<java.util.Map<String, String>> glossary = new java.util.ArrayList<>();
            java.util.Map<String, String> methodEntry = new java.util.HashMap<>();
            methodEntry.put("term", "생성방법");
            methodEntry.put("definition", "AI 추론 분석 (RSS 메타데이터 기반)");
            glossary.add(methodEntry);
            summary.setGlossary(glossary);
            
            System.out.println("AI 추론 요약 성공: " + article.getUrl() + " (점수: " + importanceScore + ")");
            return summary;
            
        } catch (Exception e) {
            System.err.println("AI 추론 결과 파싱 실패: " + e.getMessage());
            return createBasicInferenceSummary(article, model, inferenceResult);
        }
    }
    
    /**
     * 섹션에서 실제 내용 추출 (다음 섹션 시작 전까지)
     */
    private String extractContent(String section) {
        String[] lines = section.split("\n");
        if (lines.length > 0) {
            // 첫 번째 라인에서 콜론 이후 내용 또는 전체 내용
            String firstLine = lines[0];
            int colonIndex = firstLine.indexOf(":");
            if (colonIndex != -1 && colonIndex < firstLine.length() - 1) {
                return firstLine.substring(colonIndex + 1).trim();
            } else {
                return firstLine.trim();
            }
        }
        return section.trim();
    }
    
    /**
     * 파싱 실패 시 기본 추론 요약 생성
     */
    private Summary createBasicInferenceSummary(Article article, String model, String inferenceResult) {
        String summaryText = "제목: " + article.getTitle();
        if (article.getRawExcerpt() != null) {
            summaryText += " | " + article.getRawExcerpt();
        }
        
        String analysisText = "AI 추론 분석 결과:\n" + inferenceResult;
        
        Summary summary = new Summary(article, model, summaryText, analysisText);
        summary.setScore(BigDecimal.valueOf(5)); // 기본 점수
        summary.setBulletsList(extractKeywordsFromTitle(article.getTitle()));
        
        return summary;
    }
    
    /**
     * EconomicSummaryResponse를 Summary 엔티티로 변환
     */
    private Summary convertToSummary(Article article, String model, EconomicSummaryResponse aiResponse) {
        Summary summary = new Summary(article, model, aiResponse.getSummary(), aiResponse.getAnalysis());
        
        // 점수 설정
        if (aiResponse.getImportanceScore() != null) {
            summary.setScore(BigDecimal.valueOf(aiResponse.getImportanceScore()));
        }
        
        // 키워드를 bullets로 저장 (빈 리스트라도 설정)
        if (aiResponse.getKeywords() != null && !aiResponse.getKeywords().isEmpty()) {
            summary.setBulletsList(aiResponse.getKeywords());
        } else {
            // 키워드가 없으면 기본 키워드 설정
            summary.setBulletsList(java.util.Arrays.asList("경제뉴스"));
        }
        
        // 경제 섹터와 추가 정보를 glossary로 저장
        if (aiResponse.getEconomicSectors() != null || aiResponse.getContext() != null) {
            List<java.util.Map<String, String>> glossary = new java.util.ArrayList<>();
            
            // 시장 영향
            if (aiResponse.getMarketImpact() != null) {
                java.util.Map<String, String> marketEntry = new java.util.HashMap<>();
                marketEntry.put("term", "시장영향");
                marketEntry.put("definition", aiResponse.getMarketImpact());
                glossary.add(marketEntry);
            }
            
            // 투자자 관심도
            if (aiResponse.getInvestorInterest() != null) {
                java.util.Map<String, String> interestEntry = new java.util.HashMap<>();
                interestEntry.put("term", "투자자관심");
                interestEntry.put("definition", aiResponse.getInvestorInterest());
                glossary.add(interestEntry);
            }
            
            // 경제 섹터
            if (aiResponse.getEconomicSectors() != null && !aiResponse.getEconomicSectors().isEmpty()) {
                java.util.Map<String, String> sectorEntry = new java.util.HashMap<>();
                sectorEntry.put("term", "관련섹터");
                sectorEntry.put("definition", String.join(", ", aiResponse.getEconomicSectors()));
                glossary.add(sectorEntry);
            }
            
            // 신뢰도 점수
            if (aiResponse.getConfidenceScore() != null) {
                java.util.Map<String, String> confidenceEntry = new java.util.HashMap<>();
                confidenceEntry.put("term", "신뢰도");
                confidenceEntry.put("definition", aiResponse.getConfidenceScore().toString() + "/10");
                glossary.add(confidenceEntry);
            }
            
            // 추가 컨텍스트
            if (aiResponse.getContext() != null && !aiResponse.getContext().trim().isEmpty()) {
                java.util.Map<String, String> contextEntry = new java.util.HashMap<>();
                contextEntry.put("term", "배경정보");
                contextEntry.put("definition", aiResponse.getContext());
                glossary.add(contextEntry);
            }
            
            if (!glossary.isEmpty()) {
                summary.setGlossary(glossary);
            }
        }
        
        return summary;
    }
    
    /**
     * 폴백 요약 생성 (AI 실패 시)
     */
    private Summary createFallbackSummary(Article article, String model, String errorMessage) {
        // 기본 요약 생성 (제목과 설명 기반)
        String fallbackSummary = generateBasicSummary(article);
        String fallbackAnalysis;
        BigDecimal score;
        
        // RSS 메타데이터 기반인 경우 더 높은 점수 부여
        if (errorMessage.contains("RSS 메타데이터")) {
            fallbackAnalysis = "RSS 피드의 제목과 요약 정보를 기반으로 생성된 요약입니다.";
            score = BigDecimal.valueOf(5); // 보통 점수
        } else {
            fallbackAnalysis = "자동 분석이 불가능하여 기본 정보로 대체되었습니다. 오류: " + errorMessage;
            score = BigDecimal.valueOf(3); // 낮은 점수
        }
        
        Summary summary = new Summary(article, model, fallbackSummary, fallbackAnalysis);
        summary.setScore(score);
        
        // 기본 키워드 설정 (제목에서 추출)
        List<String> basicKeywords = extractKeywordsFromTitle(article.getTitle());
        if (!basicKeywords.isEmpty()) {
            summary.setBulletsList(basicKeywords);
        } else {
            // 키워드 추출에 실패해도 기본값 설정
            summary.setBulletsList(java.util.Arrays.asList("경제뉴스"));
        }
        
        System.out.println("폴백 요약 생성: " + article.getUrl() + " - " + errorMessage);
        
        return summary;
    }
    
    /**
     * 기본 요약 생성
     */
    private String generateBasicSummary(Article article) {
        StringBuilder summary = new StringBuilder();
        
        if (article.getTitle() != null) {
            summary.append("제목: ").append(article.getTitle()).append(". ");
        }
        
        if (article.getRawExcerpt() != null && !article.getRawExcerpt().trim().isEmpty()) {
            summary.append(article.getRawExcerpt());
        } else if (article.getContent() != null && article.getContent().length() > 100) {
            // 본문의 앞 200자를 요약으로 사용
            String preview = article.getContent().substring(0, Math.min(200, article.getContent().length()));
            summary.append(preview).append("...");
        } else {
            summary.append("상세한 요약을 생성할 수 없습니다.");
        }
        
        return summary.toString();
    }
    
    /**
     * 제목에서 기본 키워드 추출
     */
    private List<String> extractKeywordsFromTitle(String title) {
        List<String> keywords = new java.util.ArrayList<>();
        
        if (title == null || title.trim().isEmpty()) {
            return keywords;
        }
        
        // 한국어 경제 관련 키워드 패턴 매칭
        String[] economicPatterns = {
            "경제", "성장", "금리", "환율", "주가", "증시", "부동산", "투자", "수출", "수입",
            "GDP", "인플레이션", "물가", "소비", "생산", "고용", "실업", "정책", "은행", "기업",
            "시장", "산업", "제조업", "서비스업", "금융", "증권", "채권", "주식"
        };
        
        for (String pattern : economicPatterns) {
            if (title.contains(pattern)) {
                keywords.add(pattern);
                if (keywords.size() >= 5) break; // 최대 5개까지
            }
        }
        
        if (keywords.isEmpty()) {
            keywords.add("경제뉴스"); // 기본 키워드
        }
        
        return keywords;
    }
}