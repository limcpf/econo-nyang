package com.yourco.econdigest.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.yourco.econdigest.config.RssSourcesConfig;
import com.yourco.econdigest.dto.ArticleDto;
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
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    /**
     * 단일 RSS 소스에서 기사 목록을 수집
     */
    public List<ArticleDto> fetchArticles(RssSourcesConfig.RssSource source) {
        List<ArticleDto> articles = new ArrayList<>();
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                SyndFeed feed = fetchFeed(source);
                articles = parseFeed(feed, source);
                break; // 성공 시 루프 종료
                
            } catch (Exception e) {
                System.err.println("RSS 피드 수집 실패 (시도 " + attempt + "/" + MAX_RETRIES + "): " + 
                                 source.getUrl() + " - " + e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * attempt);
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
        
        // URL 기반 중복 제거
        Set<ArticleDto> uniqueArticles = new LinkedHashSet<>(allArticles);
        List<ArticleDto> result = new ArrayList<>(uniqueArticles);
        
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
        
        // 타임아웃 설정 (기본값 사용)
        connection.setConnectTimeout(10000); // 10초
        connection.setReadTimeout(30000);    // 30초
        
        // User-Agent 설정
        connection.setRequestProperty("User-Agent", "EconDigest/1.0 (+https://github.com/yourco/econdigest)");
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
        
        for (SyndEntry entry : feed.getEntries()) {
            try {
                ArticleDto article = parseEntry(entry, source);
                if (article != null) {
                    articles.add(article);
                }
            } catch (Exception e) {
                System.err.println("기사 파싱 실패: " + entry.getLink() + " - " + e.getMessage());
            }
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
                .filter(article -> passesIncludeFilter(article, filters.getIncludeKeywords()))
                .filter(article -> passesExcludeFilter(article, filters.getExcludeKeywords()))
                .collect(Collectors.toList());
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
}