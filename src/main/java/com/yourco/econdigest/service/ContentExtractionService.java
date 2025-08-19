package com.yourco.econdigest.service;

import com.yourco.econdigest.dto.ArticleDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 웹 페이지에서 본문 내용을 추출하는 서비스
 */
@Service
public class ContentExtractionService {
    
    private static final int DEFAULT_TIMEOUT = 10000; // 10초
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1000;
    
    // 한국 언론사별 본문 선택자 매핑
    private static final String[][] CONTENT_SELECTORS = {
        // 한국경제
        {"hankyung", "div.article-body, div.news-text, div.article-contents"},
        // 매일경제  
        {"maeil", "div.news_cnt_detail, div.article_txt"},
        // 연합뉴스
        {"yonhap", "div.story-news-article, div.article-story"},
        // 조선일보
        {"chosun", "div.par, div.article-body"},
        // 중앙일보
        {"joongang", "div.article_body, div#article_body"},
        // 동아일보
        {"donga", "div.article_txt, div.news_view"},
        // 일반적인 선택자 (마지막 fallback)
        {"default", "article, div.content, div.article, div.entry-content, div.post-content, " +
                   "div.article-content, div.story-body, div.article-body, " +
                   "div#content, div#article, div.text, p"}
    };
    
    // 제거할 요소들
    private static final String[] REMOVE_SELECTORS = {
        "script", "style", "nav", "header", "footer", "aside", ".advertisement", 
        ".ads", ".related", ".comment", ".social", ".share", ".tags",
        ".author-info", ".article-footer", ".sidebar"
    };
    
    /**
     * 단일 기사의 본문을 추출
     */
    public ArticleDto extractContent(ArticleDto article) {
        if (article == null) {
            return null;
        }
        
        if (article.getUrl() == null) {
            article.setExtractSuccess(false);
            article.setExtractError("기사 URL이 없습니다");
            return article;
        }
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String content = fetchAndExtractContent(article.getUrl(), article.getSource());
                
                if (content != null && !content.trim().isEmpty()) {
                    article.setContent(content);
                    article.setExtractedAt(LocalDateTime.now());
                    article.setExtractSuccess(true);
                    article.setExtractError(null);
                    return article;
                }
                
            } catch (Exception e) {
                System.err.println("본문 추출 실패 (시도 " + attempt + "/" + MAX_RETRIES + "): " + 
                                 article.getUrl() + " - " + e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    article.setExtractSuccess(false);
                    article.setExtractError(e.getMessage());
                    article.setExtractedAt(LocalDateTime.now());
                }
            }
        }
        
        return article;
    }
    
    /**
     * 여러 기사의 본문을 추출 (병렬 처리 없이 순차 처리)
     */
    public List<ArticleDto> extractContents(List<ArticleDto> articles) {
        List<ArticleDto> results = new ArrayList<>();
        
        for (ArticleDto article : articles) {
            ArticleDto extracted = extractContent(article);
            results.add(extracted);
            
            // 서버 부하 방지를 위한 짧은 대기
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return results;
    }
    
    /**
     * URL에서 실제 HTML을 가져와서 본문을 추출
     */
    private String fetchAndExtractContent(String url, String source) throws IOException {
        // Jsoup으로 웹 페이지 가져오기
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; EconDigest/1.0)")
                .timeout(DEFAULT_TIMEOUT)
                .followRedirects(true)
                .get();
        
        // 불필요한 요소들 제거
        removeUnwantedElements(doc);
        
        // 소스별 본문 추출
        String content = extractContentBySource(doc, source);
        
        // 본문 정제
        return cleanContent(content);
    }
    
    /**
     * 불필요한 HTML 요소들 제거
     */
    private void removeUnwantedElements(Document doc) {
        for (String selector : REMOVE_SELECTORS) {
            doc.select(selector).remove();
        }
    }
    
    /**
     * 소스별 본문 선택자를 사용하여 본문 추출
     */
    private String extractContentBySource(Document doc, String source) {
        // 소스별 선택자 찾기
        String selectors = getSelectorsForSource(source);
        
        // 선택자로 본문 추출 시도
        for (String selector : selectors.split(",")) {
            selector = selector.trim();
            Elements elements = doc.select(selector);
            
            if (!elements.isEmpty()) {
                StringBuilder content = new StringBuilder();
                for (Element element : elements) {
                    String text = element.text();
                    if (text.length() > 50) { // 의미있는 길이의 텍스트만
                        content.append(text).append("\n\n");
                    }
                }
                
                String result = content.toString().trim();
                if (result.length() > 100) { // 최소 길이 체크
                    return result;
                }
            }
        }
        
        // fallback: 전체 body에서 p 태그 텍스트 추출
        Elements paragraphs = doc.select("p");
        StringBuilder fallbackContent = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text();
            if (text.length() > 20) {
                fallbackContent.append(text).append("\n\n");
            }
        }
        
        return fallbackContent.toString().trim();
    }
    
    /**
     * 소스별 본문 선택자 반환
     */
    private String getSelectorsForSource(String source) {
        if (source == null) {
            return getDefaultSelectors();
        }
        
        for (String[] mapping : CONTENT_SELECTORS) {
            if (mapping[0].equals(source)) {
                return mapping[1];
            }
        }
        
        return getDefaultSelectors();
    }
    
    /**
     * 기본 선택자 반환
     */
    private String getDefaultSelectors() {
        for (String[] mapping : CONTENT_SELECTORS) {
            if ("default".equals(mapping[0])) {
                return mapping[1];
            }
        }
        return "article, div.content, p";
    }
    
    /**
     * 추출된 본문 내용 정제
     */
    private String cleanContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        // 연속된 공백과 줄바꿈 정리
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("\\n\\s*\\n", "\n\n");
        
        // 앞뒤 공백 제거
        content = content.trim();
        
        // 최대 길이 제한 (10,000자)
        if (content.length() > 10000) {
            content = content.substring(0, 10000) + "...";
        }
        
        return content;
    }
    
    /**
     * 추출 성공률 계산
     */
    public double calculateSuccessRate(List<ArticleDto> articles) {
        if (articles == null || articles.isEmpty()) {
            return 0.0;
        }
        
        long successCount = articles.stream()
                .mapToLong(article -> article.isExtractSuccess() ? 1 : 0)
                .sum();
        
        return (double) successCount / articles.size();
    }
}