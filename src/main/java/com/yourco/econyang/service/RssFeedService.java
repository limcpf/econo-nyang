package com.yourco.econyang.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.domain.Article;
import com.yourco.econyang.strategy.RssTimeFilterStrategy;
import com.yourco.econyang.strategy.RssTimeFilterStrategyFactory;
import com.yourco.econyang.util.ArticleIdExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * RSS 피드 수집 서비스
 */
@Service
public class RssFeedService {
    
    private final RssSourcesConfig rssSourcesConfig;
    
    @Autowired
    private RssTimeFilterStrategyFactory timeFilterFactory;
    
    @Autowired
    private EconomicNewsClassifier economicNewsClassifier;
    
    @Autowired
    private SmartDateFilterService smartDateFilterService;
    
    private static final String DEBUG_DIR = "debug/rss";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    public RssFeedService(RssSourcesConfig rssSourcesConfig) {
        this.rssSourcesConfig = rssSourcesConfig;
    }
    
    /**
     * 단일 RSS 소스에서 기사 목록을 수집
     */
    public List<ArticleDto> fetchArticles(RssSourcesConfig.RssSource source) {
        List<ArticleDto> articles = new ArrayList<>();
        
        int maxRetries = rssSourcesConfig.getCollection().getMaxRetries();
        int retryDelayMs = rssSourcesConfig.getCollection().getRetryDelayMs();
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Investing.com은 첫 시도 전에 랜덤 지연 추가
                if (source.getCode().startsWith("investing_")) {
                    int randomDelay = 1000 + (int)(Math.random() * 2000); // 1-3초 랜덤 지연
                    try {
                        TimeUnit.MILLISECONDS.sleep(randomDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                SyndFeed feed = fetchFeed(source);
                articles = parseFeed(feed, source);
                break; // 성공 시 루프 종료
                
            } catch (Exception e) {
                System.err.println("RSS 피드 수집 실패 (시도 " + attempt + "/" + maxRetries + "): " + 
                                 source.getUrl() + " - " + e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // Investing.com은 더 긴 재시도 지연
                        int delay = source.getCode().startsWith("investing_") ? 
                                   retryDelayMs * attempt * 2 : retryDelayMs * attempt;
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("RSS 피드 수집 최종 실패: " + source.getUrl());
                }
            }
        }
        
        return articles;
    }
    
    /**
     * 여러 RSS 소스에서 기사 목록을 수집하고 중복 제거
     */
    public List<ArticleDto> fetchAllArticles(List<RssSourcesConfig.RssSource> sources, int maxArticles) {
        List<ArticleDto> allArticles = new ArrayList<>();
        
        for (RssSourcesConfig.RssSource source : sources) {
            if (!source.isEnabled()) {
                System.out.println("RSS 소스 비활성화: " + source.getName());
                continue;
            }
            
            System.out.println("RSS 피드 수집 시작: " + source.getName() + " (" + source.getUrl() + ")");
            List<ArticleDto> articles = fetchArticles(source);
            System.out.println("RSS 피드 수집 완료: " + source.getName() + " - " + articles.size() + "개 기사");
            
            allArticles.addAll(articles);
        }
        
        // 중복 제거 (config 기반)
        List<ArticleDto> result = removeDuplicates(allArticles, rssSourcesConfig.getCollection().getDeduplication());
        
        System.out.println("중복 제거 후 총 " + result.size() + "개 기사");
        
        // 발행일자가 있는 기사와 없는 기사로 분리
        List<ArticleDto> articlesWithDate = new ArrayList<>();
        List<ArticleDto> articlesWithoutDate = new ArrayList<>();
        
        for (ArticleDto article : result) {
            if (article.getPublishedAt() != null) {
                articlesWithDate.add(article);
            } else {
                articlesWithoutDate.add(article);
            }
        }
        
        System.out.println("발행일자 있는 기사: " + articlesWithDate.size() + "개");
        System.out.println("발행일자 없는 기사: " + articlesWithoutDate.size() + "개");
        
        // 발행일자가 없는 기사들에 대해 스마트 전략으로 24시간 이내 기사 필터링
        if (!articlesWithoutDate.isEmpty()) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            List<ArticleDto> validArticlesFromSmartFilter = smartDateFilterService
                    .filterArticlesWithSmartStrategies(articlesWithoutDate, cutoffTime);
            
            System.out.println("스마트 필터링으로 유효 확인된 기사: " + validArticlesFromSmartFilter.size() + "개");
            articlesWithDate.addAll(validArticlesFromSmartFilter);
        }
        
        // 발행 시간 기준 내림차순 정렬 (최신순)
        articlesWithDate.sort((a, b) -> {
            if (a.getPublishedAt() == null && b.getPublishedAt() == null) return 0;
            if (a.getPublishedAt() == null) return 1;
            if (b.getPublishedAt() == null) return -1;
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });
        
        // 최대 기사 수 제한
        if (articlesWithDate.size() > maxArticles) {
            articlesWithDate = articlesWithDate.subList(0, maxArticles);
        }
        
        System.out.println("최종 수집 완료: " + articlesWithDate.size() + "개 기사");
        return articlesWithDate;
    }
    
    /**
     * RSS 피드를 가져와서 파싱
     */
    private SyndFeed fetchFeed(RssSourcesConfig.RssSource source) throws Exception {
        URL feedUrl = new URL(source.getUrl());
        URLConnection connection = feedUrl.openConnection();
        
        // 타임아웃 설정 (config에서 읽기)
        int connectTimeoutMs = rssSourcesConfig.getCollection().getConnectionTimeoutSec() * 1000;
        int readTimeoutMs = rssSourcesConfig.getCollection().getReadTimeoutSec() * 1000;
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        
        // User-Agent 설정 (Investing.com 특별 처리)
        String userAgent;
        if (source.getCode().startsWith("investing_")) {
            // Investing.com용 일반적인 브라우저 User-Agent
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("DNT", "1");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            connection.setRequestProperty("Sec-Fetch-Dest", "document");
            connection.setRequestProperty("Sec-Fetch-Mode", "navigate");
            connection.setRequestProperty("Sec-Fetch-Site", "none");
            connection.setRequestProperty("Cache-Control", "max-age=0");
        } else {
            // 다른 소스는 기본 User-Agent
            userAgent = rssSourcesConfig.getCollection().getUserAgent();
            connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
        }
        connection.setRequestProperty("User-Agent", userAgent);
        
        // XML 원본을 바이트 배열로 읽어서 파일 저장과 파싱에 모두 사용
        byte[] xmlData;
        try (InputStream inputStream = connection.getInputStream()) {
            xmlData = inputStream.readAllBytes();
        }
        
        // 원본 RSS XML을 디버그 파일로 저장
        saveRssXmlToFile(source.getCode(), xmlData);
        
        // 바이트 배열에서 XML 파싱
        try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlData);
             XmlReader reader = new XmlReader(bais)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(reader);
        }
    }
    
    /**
     * SyndFeed를 ArticleDto 리스트로 변환
     */
    private List<ArticleDto> parseFeed(SyndFeed feed, RssSourcesConfig.RssSource source) {
        List<ArticleDto> articles = new ArrayList<>();
        
        if (feed.getEntries() == null) {
            return articles;
        }
        
        // 시간 필터링 전략 가져오기
        RssTimeFilterStrategy timeFilterStrategy = timeFilterFactory.getStrategy(source.getCode());
        int filteredCount = 0;
        
        for (SyndEntry entry : feed.getEntries()) {
            try {
                ArticleDto articleDto = parseEntry(entry, source);
                if (articleDto != null) {
                    // ArticleDto를 임시 Article로 변환해서 시간 필터링 적용
                    Article tempArticle = new Article(source.getCode(), articleDto.getUrl(), articleDto.getTitle());
                    tempArticle.setPublishedAt(articleDto.getPublishedAt());
                    
                    // 시간 필터링 검사
                    if (timeFilterStrategy.shouldInclude(tempArticle, source.getCode())) {
                        articles.add(articleDto);
                    } else {
                        filteredCount++;
                        System.out.println(String.format("⏰ 시간 필터링 제외: [%s] %s - 발행일: %s (기준: %d시간 이내)", 
                            source.getCode(), 
                            truncate(articleDto.getTitle(), 50),
                            articleDto.getPublishedAt() != null ? articleDto.getPublishedAt().toString() : "없음",
                            timeFilterStrategy.getMaxAgeHours(source.getCode())
                        ));
                    }
                }
            } catch (Exception e) {
                System.err.println("기사 파싱 실패: " + entry.getLink() + " - " + e.getMessage());
            }
        }
        
        if (filteredCount > 0) {
            System.out.println(String.format(
                "RSS [%s] 시간 필터링: %d개 기사 제외 (전략: %s, 기준: %d시간)", 
                source.getCode(), 
                filteredCount, 
                timeFilterStrategy.getStrategyName(),
                timeFilterStrategy.getMaxAgeHours(source.getCode())
            ));
        }
        
        // 파싱 결과를 디버그 파일로 저장
        saveParsedResultToFile(source.getCode(), feed, articles, filteredCount);
        
        // 필터링 로그를 별도 파일로 저장
        saveFilteringLogToFile(source.getCode(), feed.getEntries().size(), articles.size(), filteredCount);
        
        return articles;
    }
    
    /**
     * SyndEntry를 ArticleDto로 변환
     */
    private ArticleDto parseEntry(SyndEntry entry, RssSourcesConfig.RssSource source) {
        // URL 검증
        String url = entry.getLink();
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        // URL 정규화
        url = url.trim();
        if (!url.startsWith("http")) {
            return null;
        }
        
        // 제목 검증
        String title = entry.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        
        ArticleDto article = new ArticleDto();
        article.setSource(source.getName());
        article.setUrl(url);
        article.setTitle(title.trim());
        article.setSourceWeight(1.0); // 기본 가중치
        
        // RSS별 고유 식별자 생성
        String uniqueId = ArticleIdExtractor.extractUniqueId(source.getCode(), url);
        article.setUniqueId(uniqueId);
        
        // 설명/요약
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            article.setDescription(entry.getDescription().getValue().trim());
        }
        
        // 작성자
        if (entry.getAuthor() != null && !entry.getAuthor().trim().isEmpty()) {
            article.setAuthor(entry.getAuthor().trim());
        }
        
        // 발행 시간
        if (entry.getPublishedDate() != null) {
            LocalDateTime publishedAt = entry.getPublishedDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            article.setPublishedAt(publishedAt);
        } else {
            // 발행일이 없는 경우 null로 두고 로그만 기록
            article.setPublishedAt(null);
            System.out.println("기사 발행일 누락 (null로 설정): " + url);
        }
        
        return article;
    }
    
    /**
     * 키워드 필터링 적용
     */
    public List<ArticleDto> applyFilters(List<ArticleDto> articles, RssSourcesConfig.FilterConfig filters) {
        if (filters == null) {
            return articles;
        }
        
        List<ArticleDto> result = new ArrayList<>();
        int titleLengthFiltered = 0, includeKeywordFiltered = 0, excludeKeywordFiltered = 0, economicQualityFiltered = 0;
        
        for (ArticleDto article : articles) {
            boolean passed = true;
            String reason = null;
            
            // 1. 제목 길이 필터
            if (!passesTitleLengthFilter(article, filters)) {
                passed = false;
                reason = String.format("제목 길이 부적합 (%d자, 범위: %d-%d자)", 
                    article.getTitle() != null ? article.getTitle().length() : 0,
                    filters.getMinTitleLength(), filters.getMaxTitleLength());
                titleLengthFiltered++;
            }
            // 2. 고급 경제 뉴스 분류 필터 (새로 추가)
            else if (!economicNewsClassifier.shouldIncludeNews(article, 2)) { // 최소 점수 2점
                passed = false;
                reason = "경제 관련성 부족 (고급 분류)";
                economicQualityFiltered++;
            }
            // 3. 기존 포함 키워드 필터 (보조적 역할)
            else if (!passesIncludeFilter(article, filters.getIncludeKeywords())) {
                passed = false;
                reason = "포함 키워드 불일치: " + filters.getIncludeKeywords();
                includeKeywordFiltered++;
            }
            // 4. 기존 제외 키워드 필터 (보조적 역할)  
            else if (!passesExcludeFilter(article, filters.getExcludeKeywords())) {
                passed = false;
                reason = "제외 키워드 포함: " + filters.getExcludeKeywords();
                excludeKeywordFiltered++;
            }
            
            if (passed) {
                result.add(article);
            }
            // 로깅은 EconomicNewsClassifier에서 이미 처리됨
        }
        
        if (titleLengthFiltered > 0 || includeKeywordFiltered > 0 || excludeKeywordFiltered > 0 || economicQualityFiltered > 0) {
            System.out.println(String.format("📊 콘텐츠 필터링 결과: 제목길이 %d개, 경제품질 %d개, 포함키워드 %d개, 제외키워드 %d개 제외", 
                titleLengthFiltered, economicQualityFiltered, includeKeywordFiltered, excludeKeywordFiltered));
        }
        
        return result;
    }
    
    /**
     * 제목 길이 필터
     */
    private boolean passesTitleLengthFilter(ArticleDto article, RssSourcesConfig.FilterConfig filters) {
        if (article.getTitle() == null) {
            return false;
        }
        
        int titleLength = article.getTitle().length();
        return titleLength >= filters.getMinTitleLength() && 
               titleLength <= filters.getMaxTitleLength();
    }
    
    /**
     * 포함 키워드 필터 (하나라도 포함되어야 함)
     */
    private boolean passesIncludeFilter(ArticleDto article, List<String> includeKeywords) {
        if (includeKeywords == null || includeKeywords.isEmpty()) {
            return true; // 필터가 없으면 모두 통과
        }
        
        String content = (article.getTitle() + " " + 
                         (article.getDescription() != null ? article.getDescription() : "")).toLowerCase();
        
        return includeKeywords.stream()
                .anyMatch(keyword -> content.contains(keyword.toLowerCase()));
    }
    
    /**
     * 제외 키워드 필터 (하나라도 포함되면 제외)
     */
    private boolean passesExcludeFilter(ArticleDto article, List<String> excludeKeywords) {
        if (excludeKeywords == null || excludeKeywords.isEmpty()) {
            return true; // 필터가 없으면 모두 통과
        }
        
        String content = (article.getTitle() + " " + 
                         (article.getDescription() != null ? article.getDescription() : "")).toLowerCase();
        
        return excludeKeywords.stream()
                .noneMatch(keyword -> content.contains(keyword.toLowerCase()));
    }
    
    /**
     * 중복 제거 (URL 및 제목 유사도 기반)
     */
    private List<ArticleDto> removeDuplicates(List<ArticleDto> articles, RssSourcesConfig.DeduplicationConfig dedupConfig) {
        if (articles.isEmpty()) {
            return articles;
        }
        
        List<ArticleDto> result = new ArrayList<>();
        Set<String> seenUniqueIds = new HashSet<>();
        Set<String> seenUrls = new HashSet<>();
        
        for (ArticleDto article : articles) {
            boolean isDuplicate = false;
            
            // 고유 ID 기반 중복 제거 (우선순위 1)
            if (article.getUniqueId() != null) {
                if (seenUniqueIds.contains(article.getUniqueId())) {
                    isDuplicate = true;
                    System.out.println("중복 제거 (고유ID): " + article.getUniqueId() + " -> " + article.getTitle());
                } else {
                    seenUniqueIds.add(article.getUniqueId());
                }
            }
            
            // URL 기반 중복 제거 (우선순위 2)
            if (!isDuplicate && dedupConfig.isEnableUrlDedup()) {
                if (seenUrls.contains(article.getUrl())) {
                    isDuplicate = true;
                    System.out.println("중복 제거 (URL): " + article.getUrl());
                } else {
                    seenUrls.add(article.getUrl());
                }
            }
            
            // 제목 유사도 기반 중복 제거 (우선순위 3)
            if (!isDuplicate && dedupConfig.isEnableTitleDedup()) {
                for (ArticleDto existing : result) {
                    if (calculateTitleSimilarity(article.getTitle(), existing.getTitle()) >= dedupConfig.getTitleSimilarityThreshold()) {
                        isDuplicate = true;
                        System.out.println("중복 제거 (제목 유사도): " + article.getTitle());
                        break;
                    }
                }
            }
            
            if (!isDuplicate) {
                result.add(article);
            }
        }
        
        return result;
    }
    
    /**
     * 제목 유사도 계산 (간단한 Jaccard similarity)
     */
    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null) {
            return 0.0;
        }
        
        Set<String> words1 = new HashSet<>(Arrays.asList(title1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(title2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * RSS별 시간 필터링 설정 출력 (디버깅용)
     */
    public void printTimeFilterSettings() {
        if (timeFilterFactory != null) {
            timeFilterFactory.printAllTimeSettings();
        }
    }
    
    /**
     * 원본 RSS XML을 디버그 파일로 저장
     */
    private void saveRssXmlToFile(String sourceCode, byte[] xmlData) {
        try {
            Path debugDir = Paths.get(DEBUG_DIR);
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileName = String.format("rss_%s_%s.xml", sourceCode, timestamp);
            Path filePath = debugDir.resolve(fileName);
            
            Files.write(filePath, xmlData);
            System.out.println("RSS XML 저장 완료: " + filePath);
            
        } catch (IOException e) {
            System.err.println("RSS XML 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * RSS 파싱 결과를 디버그 파일로 저장
     */
    private void saveParsedResultToFile(String sourceCode, SyndFeed feed, List<ArticleDto> articles, int filteredCount) {
        try {
            Path debugDir = Paths.get(DEBUG_DIR);
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileName = String.format("parsed_%s_%s.txt", sourceCode, timestamp);
            Path filePath = debugDir.resolve(fileName);
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== RSS 피드 파싱 결과 ===\n");
            sb.append("소스 코드: ").append(sourceCode).append("\n");
            sb.append("피드 제목: ").append(feed.getTitle()).append("\n");
            sb.append("피드 설명: ").append(feed.getDescription()).append("\n");
            sb.append("파싱 시간: ").append(LocalDateTime.now()).append("\n");
            sb.append("전체 엔트리 수: ").append(feed.getEntries().size()).append("\n");
            sb.append("필터링 통과: ").append(articles.size()).append("개\n");
            sb.append("필터링 제외: ").append(filteredCount).append("개\n");
            sb.append("\n=== 파싱된 기사 목록 ===\n");
            
            for (int i = 0; i < articles.size(); i++) {
                ArticleDto article = articles.get(i);
                sb.append(String.format("\n[%d] %s\n", i + 1, article.getTitle()));
                sb.append("URL: ").append(article.getUrl()).append("\n");
                sb.append("발행시간: ").append(article.getPublishedAt()).append("\n");
                sb.append("요약: ").append(truncate(article.getDescription(), 200)).append("\n");
                sb.append("---\n");
            }
            
            Files.write(filePath, sb.toString().getBytes());
            System.out.println("RSS 파싱 결과 저장 완료: " + filePath);
            
        } catch (IOException e) {
            System.err.println("RSS 파싱 결과 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * 필터링 로그를 파일로 저장
     */
    private void saveFilteringLogToFile(String sourceCode, int totalEntries, int validArticles, int filteredCount) {
        try {
            Path debugDir = Paths.get(DEBUG_DIR);
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }
            
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileName = String.format("filter_log_%s_%s.log", sourceCode, timestamp);
            Path filePath = debugDir.resolve(fileName);
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== 필터링 로그 ===\n");
            sb.append("소스: ").append(sourceCode).append("\n");
            sb.append("처리 시간: ").append(LocalDateTime.now()).append("\n");
            sb.append("총 RSS 엔트리: ").append(totalEntries).append("개\n");
            sb.append("필터링 통과: ").append(validArticles).append("개\n");
            sb.append("필터링 제외: ").append(filteredCount).append("개\n");
            sb.append("통과율: ").append(String.format("%.1f%%", (validArticles * 100.0) / totalEntries)).append("\n");
            sb.append("제외율: ").append(String.format("%.1f%%", (filteredCount * 100.0) / totalEntries)).append("\n");
            sb.append("===================\n");
            
            Files.write(filePath, sb.toString().getBytes());
            System.out.println("📄 필터링 로그 저장 완료: " + filePath);
            
        } catch (IOException e) {
            System.err.println("필터링 로그 저장 실패: " + e.getMessage());
        }
    }
    
    /**
     * 문자열을 지정된 길이로 자르기
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}