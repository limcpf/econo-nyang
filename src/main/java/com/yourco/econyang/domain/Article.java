package com.yourco.econyang.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 경제뉴스 기사 엔티티
 */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "author")
    private String author;

    @Column(name = "raw_excerpt")
    private String rawExcerpt;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "extract_error")
    private String extractError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Summary> summaries = new ArrayList<>();

    protected Article() {
    }

    public Article(String source, String url, String title) {
        this.source = source;
        this.url = url;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getAuthor() {
        return author;
    }

    public String getRawExcerpt() {
        return rawExcerpt;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }

    public String getExtractError() {
        return extractError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Summary> getSummaries() {
        return summaries;
    }

    // Setters
    public void setSource(String source) {
        this.source = source;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setRawExcerpt(String rawExcerpt) {
        this.rawExcerpt = rawExcerpt;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }

    public void setExtractError(String extractError) {
        this.extractError = extractError;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public void addSummary(Summary summary) {
        summaries.add(summary);
        summary.setArticle(this);
    }

    public void removeSummary(Summary summary) {
        summaries.remove(summary);
        summary.setArticle(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equals(url, article.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", source='" + source + '\'' +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", publishedAt=" + publishedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}