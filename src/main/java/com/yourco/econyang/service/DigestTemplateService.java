package com.yourco.econyang.service;

import com.yourco.econyang.config.DigestTemplateConfig;
import com.yourco.econyang.domain.Article;
import com.yourco.econyang.domain.Summary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 다이제스트 템플릿 처리 서비스
 * Summary 목록을 받아 마크다운 다이제스트를 생성합니다.
 */
@Service
public class DigestTemplateService {
    
    @Autowired
    private DigestTemplateConfig templateConfig;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 기본 템플릿을 사용하여 다이제스트 생성
     */
    public String generateDigest(List<Summary> summaries) {
        return generateDigest(summaries, "friendly", "markdown");
    }
    
    /**
     * 지정된 템플릿으로 다이제스트 생성
     */
    public String generateDigest(List<Summary> summaries, String templateName, String formatName) {
        if (summaries == null || summaries.isEmpty()) {
            return generateEmptyDigest(templateName);
        }
        
        // 템플릿 설정 조회
        DigestTemplateConfig.Template template = templateConfig.getTemplates().get(templateName);
        if (template == null) {
            template = templateConfig.getTemplates().get("friendly");
        }
        if (template == null) {
            template = templateConfig.getTemplates().get("default");
        }
        
        // 통계 정보 계산
        DigestStats stats = calculateStats(summaries);
        
        // 중요도순 정렬된 목록 준비 (헤드라인/링크용에도 사용)
        List<Summary> sortedSummaries = summaries.stream()
                .sorted((s1, s2) -> {
                    // score가 없으면 0으로 처리
                    BigDecimal score1 = s1.getScore() != null ? s1.getScore() : BigDecimal.ZERO;
                    BigDecimal score2 = s2.getScore() != null ? s2.getScore() : BigDecimal.ZERO;
                    return score2.compareTo(score1); // 내림차순
                })
                .collect(Collectors.toList());
        
        // 템플릿 변수 맵 생성
        Map<String, Object> variables = buildTemplateVariables(summaries, stats);
        
        // 헤드라인 및 링크 목록 구성 (템플릿에서 루프 처리 가능하도록)
        List<String> headlineList = new ArrayList<>();
        List<Map<String, String>> linkList = new ArrayList<>();
        for (int i = 0; i < sortedSummaries.size(); i++) {
            Summary s = sortedSummaries.get(i);
            Article a = s.getArticle();
            String title = a != null && a.getTitle() != null ? a.getTitle() : "제목 없음";
            String url = a != null ? a.getUrl() : null;
            int index = i + 1;
            headlineList.add(index + ". " + title);
            Map<String, String> linkItem = new HashMap<>();
            linkItem.put("index", String.valueOf(index));
            linkItem.put("title", title);
            linkItem.put("url", url != null ? url : "");
            linkList.add(linkItem);
        }
        variables.put("headlines", headlineList);
        variables.put("links", linkList);
        
        // 마크다운 생성
        StringBuilder digest = new StringBuilder();
        
        // 헤더 추가
        if (template.getHeader() != null) {
            digest.append(replaceVariables(template.getHeader(), variables));
            digest.append("\n");
        }
        for (int i = 0; i < sortedSummaries.size(); i++) {
            Summary summary = sortedSummaries.get(i);
            
            // 기사별 변수 맵 생성
            Map<String, Object> articleVars = buildArticleVariables(summary, i + 1);
            
            // 기사 항목 템플릿 적용
            if (template.getArticleItem() != null) {
                digest.append(replaceVariables(template.getArticleItem(), articleVars));
                digest.append("\n");
            }
        }
        
        // 푸터 추가
        if (template.getFooter() != null) {
            digest.append(replaceVariables(template.getFooter(), variables));
        }
        
        return digest.toString();
    }
    
    /**
     * 빈 다이제스트 생성 (기사가 없을 때)
     */
    private String generateEmptyDigest(String templateName) {
        DigestTemplateConfig.Template template = templateConfig.getTemplates().get(templateName);
        if (template == null) {
            template = templateConfig.getTemplates().get("friendly");
        }
        if (template == null) {
            template = templateConfig.getTemplates().get("default");
        }
        
        LocalDate today = LocalDate.now();
        
        StringBuilder digest = new StringBuilder();
        digest.append("# 📈 ").append(today.format(DATE_FORMAT)).append(" 경제뉴스 다이제스트\n\n");
        digest.append("오늘은 수집된 경제 뉴스가 없습니다.\n\n");
        digest.append("---\n\n");
        digest.append("🤖 EconDigest AI | ").append(LocalDateTime.now().format(DATETIME_FORMAT));
        
        return digest.toString();
    }
    
    /**
     * 통계 정보 계산
     */
    private DigestStats calculateStats(List<Summary> summaries) {
        if (summaries.isEmpty()) {
            return new DigestStats();
        }
        
        DigestStats stats = new DigestStats();
        
        // 기본 통계
        stats.totalArticles = summaries.size();
        stats.totalSummaries = summaries.size();
        
        // 점수 관련 통계
        List<BigDecimal> scores = summaries.stream()
                .map(Summary::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!scores.isEmpty()) {
            stats.avgImportance = scores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
        }
        
        // 중요도별 분류
        for (Summary summary : summaries) {
            BigDecimal score = summary.getScore() != null ? summary.getScore() : BigDecimal.ZERO;
            double scoreValue = score.doubleValue();
            
            if (scoreValue >= 9.0) {
                stats.criticalCount++;
            } else if (scoreValue >= 8.0) {
                stats.veryHighCount++;
            } else if (scoreValue >= 6.0) {
                stats.highCount++;
            } else if (scoreValue >= 4.0) {
                stats.mediumCount++;
            } else {
                stats.lowCount++;
            }
        }
        
        // 더미 데이터 (추후 AI 분석 결과로 대체)
        stats.positiveNews = (int)(summaries.size() * 0.4); // 40%
        stats.negativeNews = (int)(summaries.size() * 0.3); // 30% 
        stats.neutralNews = summaries.size() - stats.positiveNews - stats.negativeNews; // 나머지
        
        stats.totalCollected = summaries.size();
        stats.totalAnalyzed = summaries.size();
        stats.avgConfidence = BigDecimal.valueOf(7.5); // 더미
        
        return stats;
    }
    
    /**
     * 전체 템플릿 변수 맵 생성
     */
    private Map<String, Object> buildTemplateVariables(List<Summary> summaries, DigestStats stats) {
        Map<String, Object> vars = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // 날짜/시간 변수
        vars.put("date", today.format(DATE_FORMAT));
        vars.put("time", now.format(TIME_FORMAT));
        vars.put("currentDateTime", now.format(DATETIME_FORMAT));
        vars.put("nextDigestTime", "내일 아침 7시");
        
        // 기본 통계
        vars.put("totalArticles", stats.totalArticles);
        vars.put("totalSummaries", stats.totalSummaries);
        vars.put("totalCollected", stats.totalCollected);
        vars.put("totalAnalyzed", stats.totalAnalyzed);
        
        // 중요도 관련
        vars.put("minImportance", 5); // 더미
        vars.put("avgImportance", stats.avgImportance);
        vars.put("avgConfidence", stats.avgConfidence);
        
        // 영향 분석
        vars.put("positiveNews", stats.positiveNews);
        vars.put("negativeNews", stats.negativeNews);
        vars.put("neutralNews", stats.neutralNews);
        
        // 중요도별 개수
        vars.put("criticalCount", stats.criticalCount);
        vars.put("veryHighCount", stats.veryHighCount);
        vars.put("highCount", stats.highCount);
        vars.put("mediumCount", stats.mediumCount);
        vars.put("lowCount", stats.lowCount);
        
        // 백분율 (소수점 1자리)
        if (stats.totalArticles > 0) {
            vars.put("positivePercent", String.format("%.1f", (stats.positiveNews * 100.0) / stats.totalArticles));
            vars.put("negativePercent", String.format("%.1f", (stats.negativeNews * 100.0) / stats.totalArticles));
            vars.put("neutralPercent", String.format("%.1f", (stats.neutralNews * 100.0) / stats.totalArticles));
            
            vars.put("criticalPercent", String.format("%.1f", (stats.criticalCount * 100.0) / stats.totalArticles));
            vars.put("veryHighPercent", String.format("%.1f", (stats.veryHighCount * 100.0) / stats.totalArticles));
            vars.put("highPercent", String.format("%.1f", (stats.highCount * 100.0) / stats.totalArticles));
            vars.put("mediumPercent", String.format("%.1f", (stats.mediumCount * 100.0) / stats.totalArticles));
            vars.put("lowPercent", String.format("%.1f", (stats.lowCount * 100.0) / stats.totalArticles));
        }
        
        // 추출/분석 성공률 (더미)
        vars.put("extractSuccessRate", "95.0");
        vars.put("analysisSuccessRate", "98.0");
        vars.put("selectionRate", "80.0");
        
        // 조건부 변수
        vars.put("hasHighImportanceNews", stats.veryHighCount + stats.criticalCount > 0);
        vars.put("hasCriticalNews", stats.criticalCount > 0);
        vars.put("highImportanceCount", stats.veryHighCount + stats.criticalCount);
        
        // 종합 평가 (더미)
        vars.put("overallAssessment", "전반적으로 안정적인 경제 상황이 유지되고 있으나, 일부 정책 변화에 주목할 필요가 있습니다.");
        
        return vars;
    }
    
    /**
     * 텍스트를 문장 단위로 나누어 읽기 쉬운 리스트로 변환
     */
    private List<String> formatTextToList(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> sentences = new ArrayList<>();
        
        // 문장 분리: 온점, 느낌표, 물음표를 기준으로 분리하되 구두점은 보존
        String[] parts = text.split("(?<=[.!?])\\s+");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // 문장 끝에 구두점이 없으면 온점 추가
                if (!trimmed.matches(".*[.!?]$")) {
                    trimmed += ".";
                }
                sentences.add(trimmed);
            }
        }
        
        // 빈 리스트면 원본 텍스트를 그대로 반환
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.add(text.trim().endsWith(".") || text.trim().endsWith("!") || text.trim().endsWith("?") 
                    ? text.trim() : text.trim() + ".");
        }
        
        return sentences;
    }
    
    /**
     * 개별 기사 템플릿 변수 맵 생성
     */
    private Map<String, Object> buildArticleVariables(Summary summary, int rank) {
        Map<String, Object> vars = new HashMap<>();
        
        Article article = summary.getArticle();
        
        // 기사 기본 정보
        vars.put("title", article.getTitle());
        vars.put("url", article.getUrl());
        vars.put("source", article.getSource());
        vars.put("author", article.getAuthor() != null ? article.getAuthor() : "기자명 미상");
        
        // 시간 정보
        if (article.getPublishedAt() != null) {
            vars.put("publishedTime", article.getPublishedAt().format(TIME_FORMAT));
        } else {
            vars.put("publishedTime", "시간 미상");
        }
        
        // Summary 정보
        vars.put("aiSummary", summary.getSummaryText());
        vars.put("aiAnalysis", summary.getWhyItMatters());
        
        // 포맷된 요약과 분석 (문장별로 나누어서)
        vars.put("aiSummaryFormatted", formatTextToList(summary.getSummaryText()));
        vars.put("aiAnalysisFormatted", formatTextToList(summary.getWhyItMatters()));
        vars.put("contextFormatted", summary.getWhyItMatters() != null ? 
            formatTextToList("관련 배경: " + summary.getWhyItMatters()) : new ArrayList<>());
        
        // 점수 정보
        BigDecimal score = summary.getScore() != null ? summary.getScore() : BigDecimal.ZERO;
        vars.put("importanceScore", score.intValue());
        vars.put("confidenceScore", 8); // 더미
        
        // 중요도에 따른 별표와 순위
        int scoreInt = score.intValue();
        vars.put("importance", rank); // 순위
        
        // AI 분석 더미 데이터 (향후 Article 엔티티에 필드 추가 예정)
        String marketImpact = scoreInt >= 7 ? "높음" : scoreInt >= 5 ? "보통" : "낮음";
        vars.put("marketImpact", marketImpact);
        vars.put("investorInterest", scoreInt >= 8 ? "매우 높음" : scoreInt >= 6 ? "높음" : scoreInt >= 4 ? "보통" : "낮음");
        vars.put("economicSectors", "금융, 증권"); // 더미
        
        // 친근한 템플릿을 위한 추가 변수들
        vars.put("marketImpact_high", "높음".equals(marketImpact));
        vars.put("marketImpact_medium", "보통".equals(marketImpact));
        vars.put("marketImpact_low", "낮음".equals(marketImpact));
        
        // 키워드 생성 (bullets 배열 활용)
        List<String> bulletsList = summary.getBulletsList();
        if (!bulletsList.isEmpty()) {
            vars.put("keywords", String.join(", ", bulletsList.subList(0, Math.min(bulletsList.size(), 3))));
        } else {
            vars.put("keywords", "경제, 뉴스");
        }
        
        // 추가 컨텍스트 (선택사항)
        if (scoreInt >= 8) {
            vars.put("context", "특별히 주목할 만한 중요 뉴스입니다.");
        }
        
        // Glossary 처리 - 기사 내용에서 경제 용어 추출 (간단한 예시)
        List<Map<String, String>> glossaryList = extractGlossaryFromContent(summary);
        vars.put("glossary", glossaryList);
        
        return vars;
    }
    
    /**
     * 기사 내용에서 경제 용어 사전 추출
     */
    private List<Map<String, String>> extractGlossaryFromContent(Summary summary) {
        List<Map<String, String>> glossary = new ArrayList<>();
        
        String content = (summary.getSummaryText() + " " + summary.getWhyItMatters() + " " + summary.getArticle().getTitle()).toLowerCase();
        
        // 기본적인 경제 용어 사전 (실제로는 더 포괄적인 DB나 API 사용)
        Map<String, String> economicTerms = new HashMap<>();
        economicTerms.put("양적완화", "중앙은행이 시중에 돈의 양을 늘려 경기를 부양하는 정책입니다.");
        economicTerms.put("기준금리", "중앙은행이 시중은행에 돈을 빌려줄 때 적용하는 기본 금리입니다.");
        economicTerms.put("cpi", "소비자물가지수로, 일반 소비자가 구입하는 상품과 서비스의 가격 변동을 나타냅니다.");
        economicTerms.put("gdp", "국내총생산으로, 한 나라에서 1년 동안 생산된 모든 상품과 서비스의 총 가치입니다.");
        economicTerms.put("인플레이션", "물가가 지속적으로 상승하는 현상입니다.");
        economicTerms.put("디플레이션", "물가가 지속적으로 하락하는 현상입니다.");
        economicTerms.put("코스피", "한국종합주가지수로, 우리나라 대표 주식시장의 주가 동향을 나타냅니다.");
        economicTerms.put("ipo", "기업이 처음으로 주식을 공개해서 거래소에 상장하는 것입니다.");
        
        // 내용에서 용어 찾기
        for (Map.Entry<String, String> term : economicTerms.entrySet()) {
            if (content.contains(term.getKey().toLowerCase())) {
                Map<String, String> glossaryItem = new HashMap<>();
                glossaryItem.put("term", term.getKey());
                glossaryItem.put("definition", term.getValue());
                glossary.add(glossaryItem);
            }
        }
        
        return glossary;
    }
    
    /**
     * 템플릿 변수 치환 (Handlebars 스타일 지원)
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        String result = template;
        
        // 1. 먼저 반복문 처리 ({{#variableName}}...{{/variableName}})
        result = processLoopTemplates(result, variables);
        
        // 2. 조건문 처리 ({{#if condition}}...{{/if}})
        result = processConditionalTemplates(result, variables);
        
        // 3. 일반 변수 치환 ({{variableName}})
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * 반복문 템플릿 처리 ({{#listVariable}}...{{/listVariable}})
     */
    private String processLoopTemplates(String template, Map<String, Object> variables) {
        String result = template;
        
        // 여러 개의 반복문을 처리하기 위해 while 루프 사용
        while (true) {
            boolean processed = false;
            
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String varName = entry.getKey();
                Object varValue = entry.getValue();
                
                // List 타입인 경우만 처리
                if (varValue instanceof List) {
                    List<?> list = (List<?>) varValue;
                    
                    String startTag = "{{#" + varName + "}}";
                    String endTag = "{{/" + varName + "}}";
                    
                    int startIndex = result.indexOf(startTag);
                    if (startIndex != -1) {
                        int endIndex = result.indexOf(endTag, startIndex);
                        if (endIndex != -1) {
                            String beforeLoop = result.substring(0, startIndex);
                            String loopTemplate = result.substring(startIndex + startTag.length(), endIndex);
                            String afterLoop = result.substring(endIndex + endTag.length());
                            
                            StringBuilder loopResult = new StringBuilder();
                            for (Object item : list) {
                                String itemContent = loopTemplate;
                                
                                if (item instanceof String) {
                                    // 문자열 리스트의 경우
                                    itemContent = itemContent.replace("{{this}}", (String) item);
                                } else if (item instanceof Map) {
                                    // Map 리스트의 경우 (예: glossary)
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> itemMap = (Map<String, String>) item;
                                    for (Map.Entry<String, String> mapEntry : itemMap.entrySet()) {
                                        String placeholder = "{{" + mapEntry.getKey() + "}}";
                                        itemContent = itemContent.replace(placeholder, mapEntry.getValue());
                                    }
                                }
                                
                                loopResult.append(itemContent);
                            }
                            
                            result = beforeLoop + loopResult.toString() + afterLoop;
                            processed = true;
                            break; // 한 번에 하나씩 처리
                        }
                    }
                }
            }
            
            // 더 이상 처리할 반복문이 없으면 종료
            if (!processed) {
                break;
            }
        }
        
        return result;
    }
    
    /**
     * 조건문 템플릿 처리 ({{#if variable}}...{{/if}})
     */
    private String processConditionalTemplates(String template, Map<String, Object> variables) {
        String result = template;
        
        // 모든 가능한 {{#if ...}} 패턴을 찾아서 처리
        while (true) {
            int startIndex = result.indexOf("{{#if ");
            if (startIndex == -1) break;
            
            int tagEndIndex = result.indexOf("}}", startIndex);
            if (tagEndIndex == -1) break;
            
            String fullStartTag = result.substring(startIndex, tagEndIndex + 2);
            String varName = fullStartTag.substring(6, fullStartTag.length() - 2).trim(); // "{{#if " 제거하고 "}}" 제거
            
            String endTag = "{{/if}}";
            int endIndex = result.indexOf(endTag, tagEndIndex);
            if (endIndex == -1) break;
            
            String beforeCondition = result.substring(0, startIndex);
            String conditionTemplate = result.substring(tagEndIndex + 2, endIndex);
            String afterCondition = result.substring(endIndex + endTag.length());
            
            // 조건 평가
            Object varValue = variables.get(varName);
            boolean condition = evaluateCondition(varValue);
            String conditionResult = condition ? conditionTemplate : "";
            
            result = beforeCondition + conditionResult + afterCondition;
        }
        
        return result;
    }
    
    /**
     * 조건 평가
     */
    private boolean evaluateCondition(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() > 0;
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof List) return !((List<?>) value).isEmpty();
        return true;
    }
    
    /**
     * 통계 정보를 담는 내부 클래스
     */
    private static class DigestStats {
        int totalArticles = 0;
        int totalSummaries = 0;
        int totalCollected = 0;
        int totalAnalyzed = 0;
        
        BigDecimal avgImportance = BigDecimal.ZERO;
        BigDecimal avgConfidence = BigDecimal.ZERO;
        
        int positiveNews = 0;
        int negativeNews = 0;
        int neutralNews = 0;
        
        int criticalCount = 0;
        int veryHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
    }
    
    /**
     * 사용 가능한 템플릿 이름 목록 반환
     */
    public Set<String> getAvailableTemplates() {
        return templateConfig.getTemplates().keySet();
    }
    
    /**
     * 특정 템플릿의 타이틀 반환
     */
    public String getTemplateTitle(String templateName) {
        DigestTemplateConfig.Template template = templateConfig.getTemplates().get(templateName);
        return template != null ? template.getTitle() : "경제뉴스 다이제스트";
    }
}
