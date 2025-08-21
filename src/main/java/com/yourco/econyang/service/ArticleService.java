package com.yourco.econyang.service;

import com.yourco.econyang.domain.Article;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 기사 관련 비즈니스 로직 서비스
 */
@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * URL 기반 멱등성 저장 (Upsert)
     */
    @Transactional
    public Article saveOrUpdate(ArticleDto articleDto) {
        Optional<Article> existingArticle = articleRepository.findByUrl(articleDto.getUrl());
        
        if (existingArticle.isPresent()) {
            Article article = existingArticle.get();
            updateArticleFromDto(article, articleDto);
            return articleRepository.save(article);
        } else {
            Article newArticle = createArticleFromDto(articleDto);
            return articleRepository.save(newArticle);
        }
    }

    /**
     * 일괄 저장 (중복 제거 및 Upsert)
     */
    @Transactional
    public List<Article> saveOrUpdateAll(List<ArticleDto> articleDtos) {
        return articleDtos.stream()
                .map(this::saveOrUpdate)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 기사들을 중복 방지하여 저장 (ExecutionContext용)
     * @param articleDtos 저장할 기사 DTO 목록
     * @return 저장된 기사들의 ID 목록
     */
    @Transactional
    public List<Long> saveNewArticles(List<ArticleDto> articleDtos) {
        return articleDtos.stream()
                .map(this::saveOrUpdate)
                .map(Article::getId)
                .collect(Collectors.toList());
    }

    /**
     * ID 목록으로 기사 조회 (ExecutionContext용)
     * @param articleIds 조회할 기사 ID 목록
     * @return 조회된 기사 목록
     */
    public List<Article> findByIds(List<Long> articleIds) {
        return articleRepository.findAllById(articleIds);
    }

    /**
     * 본문이 추출된 기사들만 조회 (ExecutionContext용)
     * @param articleIds 조회할 기사 ID 목록
     * @return 본문이 추출된 기사 목록
     */
    public List<Article> findExtractedByIds(List<Long> articleIds) {
        return articleRepository.findAllById(articleIds)
                .stream()
                .filter(article -> article.getContent() != null && !article.getContent().trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 단일 기사 저장
     * @param article 저장할 기사
     * @return 저장된 기사
     */
    @Transactional
    public Article save(Article article) {
        return articleRepository.save(article);
    }

    /**
     * URL로 기사 조회
     */
    public Optional<Article> findByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    /**
     * 특정 기간의 기사 조회
     */
    public List<Article> findByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return articleRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(startTime, endTime);
    }

    /**
     * 본문이 있는 기사만 조회
     */
    public List<Article> findArticlesWithContent() {
        return articleRepository.findArticlesWithContent();
    }

    /**
     * 요약이 없는 기사 조회 (AI 요약 대상)
     */
    public List<Article> findArticlesWithoutSummaries() {
        return articleRepository.findArticlesWithoutSummaries();
    }

    /**
     * 소스별 최근 기사 조회
     */
    public List<Article> findRecentBySource(String source, int limit) {
        List<Article> articles = articleRepository.findRecentBySource(source);
        if (articles.size() > limit) {
            return articles.subList(0, limit);
        }
        return articles;
    }

    /**
     * 기사 통계 조회
     */
    public List<Object[]> getSourceStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return articleRepository.countBySourceAndCreatedAtBetween(startTime, endTime);
    }

    /**
     * 오래된 기사 정리
     */
    @Transactional
    public int cleanupOldArticles(LocalDateTime cutoffTime) {
        return articleRepository.deleteOlderThan(cutoffTime);
    }

    /**
     * ArticleDto에서 Article 엔티티 생성
     */
    private Article createArticleFromDto(ArticleDto dto) {
        Article article = new Article(dto.getSource(), dto.getUrl(), dto.getTitle());
        updateArticleFromDto(article, dto);
        return article;
    }

    /**
     * ArticleDto 정보로 Article 엔티티 업데이트
     */
    private void updateArticleFromDto(Article article, ArticleDto dto) {
        article.setTitle(dto.getTitle());
        article.setPublishedAt(dto.getPublishedAt());
        article.setAuthor(dto.getAuthor());
        
        // description을 rawExcerpt로 매핑
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
            article.setRawExcerpt(dto.getDescription().trim());
        }
        
        // 본문 내용이 있으면 업데이트 (null이면 기존 값 유지)
        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            article.setContent(dto.getContent().trim());
        }
    }

    /**
     * Article 엔티티를 ArticleDto로 변환
     */
    public ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setSource(article.getSource());
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setAuthor(article.getAuthor());
        dto.setContent(article.getRawExcerpt());
        dto.setExtractedAt(article.getCreatedAt());
        
        // AI 요약 정보는 Summary 엔티티에서 가져와야 하므로 여기서는 설정하지 않음
        return dto;
    }

    /**
     * ID로 기사 조회
     */
    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    /**
     * 모든 기사 조회 (페이징 필요시 Pageable 추가 고려)
     */
    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    /**
     * URL 존재 여부 확인
     */
    public boolean existsByUrl(String url) {
        return articleRepository.existsByUrl(url);
    }
}