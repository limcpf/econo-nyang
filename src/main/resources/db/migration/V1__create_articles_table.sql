-- Articles table for storing news article metadata and excerpts
CREATE TABLE IF NOT EXISTS articles (
    id BIGSERIAL PRIMARY KEY,
    source TEXT NOT NULL,
    url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    published_at TIMESTAMPTZ,
    author TEXT,
    raw_excerpt TEXT,              -- 전문 대신 요약 추출물(저작권/보관 최소화)
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for performance
CREATE INDEX idx_articles_source ON articles(source);
CREATE INDEX idx_articles_published_at ON articles(published_at);
CREATE INDEX idx_articles_created_at ON articles(created_at);

-- Comments for documentation
COMMENT ON TABLE articles IS '경제뉴스 기사 정보 저장';
COMMENT ON COLUMN articles.source IS '뉴스 소스 (예: hankyung, google_kr_econ)';
COMMENT ON COLUMN articles.url IS '기사 고유 URL (중복 방지)';
COMMENT ON COLUMN articles.title IS '기사 제목';
COMMENT ON COLUMN articles.published_at IS '기사 발행 시간 (KST 기준)';
COMMENT ON COLUMN articles.author IS '기사 작성자 (옵션)';
COMMENT ON COLUMN articles.raw_excerpt IS '추출된 본문 일부 (저작권 고려)';
COMMENT ON COLUMN articles.created_at IS '시스템 저장 시간';