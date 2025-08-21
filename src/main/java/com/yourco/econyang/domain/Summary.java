package com.yourco.econyang.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 생성 기사 요약 및 해설 엔티티
 */
@Entity
@Table(name = "summaries")
@TypeDefs({
    @TypeDef(name = "string-array", typeClass = StringArrayType.class),
    @TypeDef(name = "int-array", typeClass = IntArrayType.class),
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class Summary {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "summary_text", nullable = false)
    private String summaryText;

    @Column(name = "why_it_matters", nullable = false)
    private String whyItMatters;

    @Type(type = "string-array")
    @Column(name = "bullets", columnDefinition = "text[]")
    private String[] bullets;

    @Type(type = "jsonb")
    @Column(name = "glossary", columnDefinition = "jsonb")
    private String glossaryJson;

    @Type(type = "int-array")
    @Column(name = "evidence_idx", columnDefinition = "int[]")
    private int[] evidenceIdx;

    @Column(name = "score", precision = 10, scale = 4)
    private BigDecimal score;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Summary() {
    }

    public Summary(Article article, String model, String summaryText, String whyItMatters) {
        this.article = article;
        this.model = model;
        this.summaryText = summaryText;
        this.whyItMatters = whyItMatters;
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

    public Article getArticle() {
        return article;
    }

    public String getModel() {
        return model;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public String[] getBullets() {
        return bullets;
    }

    public String getGlossaryJson() {
        return glossaryJson;
    }

    public int[] getEvidenceIdx() {
        return evidenceIdx;
    }

    public BigDecimal getScore() {
        return score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setArticle(Article article) {
        this.article = article;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public void setWhyItMatters(String whyItMatters) {
        this.whyItMatters = whyItMatters;
    }

    public void setBullets(String[] bullets) {
        this.bullets = bullets;
    }

    public void setGlossaryJson(String glossaryJson) {
        this.glossaryJson = glossaryJson;
    }

    public void setEvidenceIdx(int[] evidenceIdx) {
        this.evidenceIdx = evidenceIdx;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods for glossary JSON handling
    public List<Map<String, String>> getGlossary() {
        if (glossaryJson == null || glossaryJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            TypeReference<List<Map<String, String>>> typeRef = new TypeReference<List<Map<String, String>>>() {};
            return objectMapper.readValue(glossaryJson, typeRef);
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setGlossary(List<Map<String, String>> glossary) {
        if (glossary == null || glossary.isEmpty()) {
            this.glossaryJson = null;
            return;
        }
        try {
            this.glossaryJson = objectMapper.writeValueAsString(glossary);
        } catch (JsonProcessingException e) {
            this.glossaryJson = null;
        }
    }

    public void setBulletsList(List<String> bulletsList) {
        if (bulletsList == null || bulletsList.isEmpty()) {
            this.bullets = null;
            return;
        }
        this.bullets = bulletsList.toArray(new String[0]);
    }

    public List<String> getBulletsList() {
        if (bullets == null) {
            return new ArrayList<>();
        }
        return java.util.Arrays.asList(bullets);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Summary summary = (Summary) o;
        return Objects.equals(article, summary.article) &&
               Objects.equals(model, summary.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(article, model);
    }

    @Override
    public String toString() {
        return "Summary{" +
                "id=" + id +
                ", model='" + model + '\'' +
                ", summaryText='" + summaryText + '\'' +
                ", score=" + score +
                ", createdAt=" + createdAt +
                '}';
    }
}