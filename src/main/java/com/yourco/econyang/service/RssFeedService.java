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

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
                SyndFeed feed = fetchFeed(source);
                articles = parseFeed(feed, source);
                break; // 성공 시 루프 종료
                
            } catch (Exception e) {
                System.err.println("RSS 피드 수집 실패 (시도 " + attempt + "/" + maxRetries + "): " + 
                                 source.getUrl() + " - " + e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelayMs * attempt);
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
        
        // 발행 시간 기준 내림차순 정렬 (최신순)
        result.sort((a, b) -> {
            if (a.getPublishedAt() == null && b.getPublishedAt() == null) return 0;
            if (a.getPublishedAt() == null) return 1;
            if (b.getPublishedAt() == null) return -1;
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });
        
        // 최대 기사 수 제한
        if (result.size() > maxArticles) {
            result = result.subList(0, maxArticles);
        }
        
        System.out.println("중복 제거 후 총 " + result.size() + "개 기사 수집 완료");
        return result;
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
        
        // User-Agent 설정 (config에서 읽기)
        String userAgent = rssSourcesConfig.getCollection().getUserAgent();
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
        
        try (XmlReader reader = new XmlReader(connection)) {
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
            // 발행일이 없는 경우 현재 시간으로 설정 (수집 시점)
            LocalDateTime now = LocalDateTime.now();
            article.setPublishedAt(now);
            System.out.println("기사 발행일 누락, 현재 시간으로 설정: " + url + " -> " + now);
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
        
        return articles.stream()
                .filter(article -> passesTitleLengthFilter(article, filters))
                .filter(article -> passesIncludeFilter(article, filters.getIncludeKeywords()))
                .filter(article -> passesExcludeFilter(article, filters.getExcludeKeywords()))
                .collect(Collectors.toList());
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
}