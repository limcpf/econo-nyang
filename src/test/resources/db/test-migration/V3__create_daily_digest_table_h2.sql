-- Daily digest table for storing final formatted digests (H2 compatible)
CREATE TABLE IF NOT EXISTS daily_digest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    digest_date DATE NOT NULL UNIQUE,
    title TEXT NOT NULL,
    body_markdown TEXT NOT NULL,   -- 최종 다이제스트 마크다운
    total_articles INTEGER DEFAULT 0,
    total_summaries INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trigger function for updating updated_at (H2 syntax)
-- Note: H2 doesn't support PostgreSQL-style triggers, so we'll handle this in the application layer

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_daily_digest_date ON daily_digest(digest_date);
CREATE INDEX IF NOT EXISTS idx_daily_digest_created_at ON daily_digest(created_at);