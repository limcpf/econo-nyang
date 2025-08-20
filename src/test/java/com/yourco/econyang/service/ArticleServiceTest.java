package com.yourco.econyang.service;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ArticleService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    private ArticleDto sampleArticleDto;
    private Article sampleArticle;

    @BeforeEach
    void setUp() {
        sampleArticleDto = new ArticleDto();
        sampleArticleDto.setSource("한국경제");
        sampleArticleDto.setUrl("https://test.com/1");
        sampleArticleDto.setTitle("테스트 기사");
        sampleArticleDto.setAuthor("기자");
        sampleArticleDto.setContent("기사 본문");
        sampleArticleDto.setPublishedAt(LocalDateTime.now());

        sampleArticle = new Article("한국경제", "https://test.com/1", "테스트 기사");
    }

    @Test
    void testSaveOrUpdate_NewArticle() {
        // Given
        when(articleRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenReturn(sampleArticle);

        // When
        Article result = articleService.saveOrUpdate(sampleArticleDto);

        // Then
        assertNotNull(result);
        verify(articleRepository).findByUrl(sampleArticleDto.getUrl());
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void testSaveOrUpdate_ExistingArticle() {
        // Given
        when(articleRepository.findByUrl(anyString())).thenReturn(Optional.of(sampleArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(sampleArticle);

        // When
        Article result = articleService.saveOrUpdate(sampleArticleDto);

        // Then
        assertNotNull(result);
        verify(articleRepository).findByUrl(sampleArticleDto.getUrl());
        verify(articleRepository).save(sampleArticle);
    }

    @Test
    void testSaveOrUpdateAll() {
        // Given
        ArticleDto dto1 = createArticleDto("https://test.com/1", "제목1");
        ArticleDto dto2 = createArticleDto("https://test.com/2", "제목2");
        List<ArticleDto> dtos = Arrays.asList(dto1, dto2);

        when(articleRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Article> results = articleService.saveOrUpdateAll(dtos);

        // Then
        assertEquals(2, results.size());
        verify(articleRepository, times(2)).findByUrl(anyString());
        verify(articleRepository, times(2)).save(any(Article.class));
    }

    @Test
    void testFindByUrl() {
        // Given
        String url = "https://test.com/1";
        when(articleRepository.findByUrl(url)).thenReturn(Optional.of(sampleArticle));

        // When
        Optional<Article> result = articleService.findByUrl(url);

        // Then
        assertTrue(result.isPresent());
        assertEquals(sampleArticle, result.get());
        verify(articleRepository).findByUrl(url);
    }

    @Test
    void testFindByDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<Article> expected = Arrays.asList(sampleArticle);
        when(articleRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(start, end))
                .thenReturn(expected);

        // When
        List<Article> result = articleService.findByDateRange(start, end);

        // Then
        assertEquals(expected, result);
        verify(articleRepository).findByPublishedAtBetweenOrderByPublishedAtDesc(start, end);
    }

    @Test
    void testFindArticlesWithContent() {
        // Given
        List<Article> expected = Arrays.asList(sampleArticle);
        when(articleRepository.findArticlesWithContent()).thenReturn(expected);

        // When
        List<Article> result = articleService.findArticlesWithContent();

        // Then
        assertEquals(expected, result);
        verify(articleRepository).findArticlesWithContent();
    }

    @Test
    void testFindArticlesWithoutSummaries() {
        // Given
        List<Article> expected = Arrays.asList(sampleArticle);
        when(articleRepository.findArticlesWithoutSummaries()).thenReturn(expected);

        // When
        List<Article> result = articleService.findArticlesWithoutSummaries();

        // Then
        assertEquals(expected, result);
        verify(articleRepository).findArticlesWithoutSummaries();
    }

    @Test
    void testExistsByUrl() {
        // Given
        String url = "https://test.com/1";
        when(articleRepository.existsByUrl(url)).thenReturn(true);

        // When
        boolean result = articleService.existsByUrl(url);

        // Then
        assertTrue(result);
        verify(articleRepository).existsByUrl(url);
    }

    @Test
    void testConvertToDto() {
        // Given
        sampleArticle.setAuthor("기자명");
        sampleArticle.setRawExcerpt("본문 내용");
        sampleArticle.setPublishedAt(LocalDateTime.now());

        // When
        ArticleDto result = articleService.convertToDto(sampleArticle);

        // Then
        assertNotNull(result);
        assertEquals(sampleArticle.getSource(), result.getSource());
        assertEquals(sampleArticle.getUrl(), result.getUrl());
        assertEquals(sampleArticle.getTitle(), result.getTitle());
        assertEquals(sampleArticle.getAuthor(), result.getAuthor());
        assertEquals(sampleArticle.getRawExcerpt(), result.getContent());
        assertEquals(sampleArticle.getPublishedAt(), result.getPublishedAt());
    }

    @Test
    void testCleanupOldArticles() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        int deletedCount = 5;
        when(articleRepository.deleteOlderThan(cutoffTime)).thenReturn(deletedCount);

        // When
        int result = articleService.cleanupOldArticles(cutoffTime);

        // Then
        assertEquals(deletedCount, result);
        verify(articleRepository).deleteOlderThan(cutoffTime);
    }

    private ArticleDto createArticleDto(String url, String title) {
        ArticleDto dto = new ArticleDto();
        dto.setSource("테스트소스");
        dto.setUrl(url);
        dto.setTitle(title);
        return dto;
    }
}