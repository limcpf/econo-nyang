-- AI summaries table for storing generated article summaries (H2 compatible)
CREATE TABLE IF NOT EXISTS summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    model TEXT NOT NULL,           -- 'gpt-4o-mini', 'gpt-4o' 등
    summary_text TEXT NOT NULL,    -- 한 줄 요약
    why_it_matters TEXT,           -- 왜 중요한지 설명
    bullets TEXT,                  -- H2에서는 쉼표로 구분된 문자열로 저장
    evidence_idx TEXT,             -- H2에서는 쉼표로 구분된 숫자 문자열로 저장
    glossary TEXT,                 -- JSON 문자열로 저장
    score DOUBLE,                  -- 0-100 중요도 점수
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_summaries_article_id ON summaries(article_id);
CREATE INDEX IF NOT EXISTS idx_summaries_model ON summaries(model);
CREATE INDEX IF NOT EXISTS idx_summaries_score ON summaries(score);
CREATE INDEX IF NOT EXISTS idx_summaries_created_at ON summaries(created_at);