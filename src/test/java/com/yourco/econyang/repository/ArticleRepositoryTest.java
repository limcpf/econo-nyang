package com.yourco.econyang.repository;

import com.yourco.econyang.domain.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleRepository JPA 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class ArticleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void testSaveAndFindById() {
        // Given
        Article article = new Article("한국경제", "https://test.com/1", "테스트 기사");
        
        // When
        Article savedArticle = articleRepository.save(article);
        
        // Then
        assertNotNull(savedArticle.getId());
        
        Optional<Article> found = articleRepository.findById(savedArticle.getId());
        assertTrue(found.isPresent());
        assertEquals("한국경제", found.get().getSource());
        assertEquals("https://test.com/1", found.get().getUrl());
    }

    @Test
    void testFindByUrl() {
        // Given
        String url = "https://test.com/unique";
        Article article = new Article("소스", url, "제목");
        entityManager.persistAndFlush(article);

        // When
        Optional<Article> found = articleRepository.findByUrl(url);

        // Then
        assertTrue(found.isPresent());
        assertEquals(url, found.get().getUrl());
    }

    @Test
    void testExistsByUrl() {
        // Given
        String url = "https://test.com/exists";
        Article article = new Article("소스", url, "제목");
        entityManager.persistAndFlush(article);

        // When & Then
        assertTrue(articleRepository.existsByUrl(url));
        assertFalse(articleRepository.existsByUrl("https://nonexistent.com"));
    }

    @Test
    void testFindBySourceOrderByPublishedAtDesc() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Article article1 = new Article("한국경제", "https://test.com/1", "제목1");
        article1.setPublishedAt(now.minusHours(2));
        
        Article article2 = new Article("한국경제", "https://test.com/2", "제목2");
        article2.setPublishedAt(now.minusHours(1));
        
        Article article3 = new Article("매일경제", "https://test.com/3", "제목3");
        article3.setPublishedAt(now);

        entityManager.persistAndFlush(article1);
        entityManager.persistAndFlush(article2);
        entityManager.persistAndFlush(article3);

        // When
        List<Article> hankyungArticles = articleRepository.findBySourceOrderByPublishedAtDesc("한국경제");

        // Then
        assertEquals(2, hankyungArticles.size());
        assertEquals("제목2", hankyungArticles.get(0).getTitle()); // 최신순
        assertEquals("제목1", hankyungArticles.get(1).getTitle());
    }

    @Test
    void testFindByPublishedAtBetween() {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);
        LocalDateTime startTime = baseTime.minusHours(1);
        LocalDateTime endTime = baseTime.plusHours(1);

        Article article1 = new Article("소스", "https://test.com/1", "제목1");
        article1.setPublishedAt(baseTime.minusHours(2)); // 범위 밖
        
        Article article2 = new Article("소스", "https://test.com/2", "제목2");
        article2.setPublishedAt(baseTime); // 범위 안
        
        Article article3 = new Article("소스", "https://test.com/3", "제목3");
        article3.setPublishedAt(baseTime.plusHours(2)); // 범위 밖

        entityManager.persistAndFlush(article1);
        entityManager.persistAndFlush(article2);
        entityManager.persistAndFlush(article3);

        // When
        List<Article> articles = articleRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(startTime, endTime);

        // Then
        assertEquals(1, articles.size());
        assertEquals("제목2", articles.get(0).getTitle());
    }

    @Test
    void testFindByTitleContaining() {
        // Given
        Article article1 = new Article("소스", "https://test.com/1", "경제성장률 발표");
        Article article2 = new Article("소스", "https://test.com/2", "스포츠 뉴스");
        Article article3 = new Article("소스", "https://test.com/3", "경제정책 변화");

        entityManager.persistAndFlush(article1);
        entityManager.persistAndFlush(article2);
        entityManager.persistAndFlush(article3);

        // When
        List<Article> articles = articleRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc("경제");

        // Then
        assertEquals(2, articles.size());
        assertTrue(articles.stream().allMatch(a -> a.getTitle().contains("경제")));
    }

    @Test
    void testFindArticlesWithContent() {
        // Given
        Article article1 = new Article("소스", "https://test.com/1", "제목1");
        article1.setRawExcerpt("본문 내용이 있는 기사");
        
        Article article2 = new Article("소스", "https://test.com/2", "제목2");
        article2.setRawExcerpt(null); // 본문 없음
        
        Article article3 = new Article("소스", "https://test.com/3", "제목3");
        article3.setRawExcerpt(""); // 빈 본문

        entityManager.persistAndFlush(article1);
        entityManager.persistAndFlush(article2);
        entityManager.persistAndFlush(article3);

        // When
        List<Article> articles = articleRepository.findArticlesWithContent();

        // Then
        assertEquals(1, articles.size());
        assertEquals("제목1", articles.get(0).getTitle());
    }
}