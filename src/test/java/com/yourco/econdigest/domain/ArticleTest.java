package com.yourco.econdigest.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Article 엔티티 단위 테스트
 */
class ArticleTest {

    @Test
    void testArticleCreation() {
        // Given
        String source = "한국경제";
        String url = "https://www.hankyung.com/economy/article/202408200001";
        String title = "경제 뉴스 제목";

        // When
        Article article = new Article(source, url, title);

        // Then
        assertNotNull(article);
        assertEquals(source, article.getSource());
        assertEquals(url, article.getUrl());
        assertEquals(title, article.getTitle());
        assertNotNull(article.getCreatedAt());
        assertTrue(article.getSummaries().isEmpty());
    }

    @Test
    void testArticleEquality() {
        // Given
        String url1 = "https://www.hankyung.com/economy/article/1";
        String url2 = "https://www.hankyung.com/economy/article/2";

        Article article1 = new Article("source1", url1, "title1");
        Article article2 = new Article("source2", url1, "title2"); // 같은 URL
        Article article3 = new Article("source1", url2, "title1"); // 다른 URL

        // Then
        assertEquals(article1, article2); // URL이 같으면 같은 기사
        assertNotEquals(article1, article3); // URL이 다르면 다른 기사
        assertEquals(article1.hashCode(), article2.hashCode());
        assertNotEquals(article1.hashCode(), article3.hashCode());
    }

    @Test
    void testAddSummary() {
        // Given
        Article article = new Article("source", "url", "title");
        Summary summary = new Summary(article, "gpt-4o", "요약", "중요한 이유");

        // When
        article.addSummary(summary);

        // Then
        assertEquals(1, article.getSummaries().size());
        assertTrue(article.getSummaries().contains(summary));
        assertEquals(article, summary.getArticle());
    }

    @Test
    void testRemoveSummary() {
        // Given
        Article article = new Article("source", "url", "title");
        Summary summary = new Summary(article, "gpt-4o", "요약", "중요한 이유");
        article.addSummary(summary);

        // When
        article.removeSummary(summary);

        // Then
        assertEquals(0, article.getSummaries().size());
        assertFalse(article.getSummaries().contains(summary));
        assertNull(summary.getArticle());
    }

    @Test
    void testSetters() {
        // Given
        Article article = new Article("source", "url", "title");
        LocalDateTime publishedAt = LocalDateTime.now().minusHours(1);
        String author = "기자명";
        String rawExcerpt = "기사 본문 일부";

        // When
        article.setPublishedAt(publishedAt);
        article.setAuthor(author);
        article.setRawExcerpt(rawExcerpt);

        // Then
        assertEquals(publishedAt, article.getPublishedAt());
        assertEquals(author, article.getAuthor());
        assertEquals(rawExcerpt, article.getRawExcerpt());
    }

    @Test
    void testToString() {
        // Given
        Article article = new Article("한국경제", "https://test.com", "제목");

        // When
        String toString = article.toString();

        // Then
        assertTrue(toString.contains("한국경제"));
        assertTrue(toString.contains("https://test.com"));
        assertTrue(toString.contains("제목"));
    }
}