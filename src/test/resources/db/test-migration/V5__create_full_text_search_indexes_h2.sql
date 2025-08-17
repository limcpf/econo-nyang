-- Full-text search indexes (H2 compatible)
-- H2 has limited full-text search capabilities compared to PostgreSQL

-- For H2, we'll create basic indexes for text search
-- In production with PostgreSQL, this would use GIN indexes with pg_trgm

-- Basic text search indexes for articles
CREATE INDEX IF NOT EXISTS idx_articles_title_search ON articles(title);
CREATE INDEX IF NOT EXISTS idx_articles_excerpt_search ON articles(raw_excerpt);

-- Basic text search indexes for summaries  
CREATE INDEX IF NOT EXISTS idx_summaries_text_search ON summaries(summary_text);
CREATE INDEX IF NOT EXISTS idx_summaries_matters_search ON summaries(why_it_matters);

-- Basic text search indexes for daily digest
CREATE INDEX IF NOT EXISTS idx_daily_digest_title_search ON daily_digest(title);
CREATE INDEX IF NOT EXISTS idx_daily_digest_body_search ON daily_digest(body_markdown);