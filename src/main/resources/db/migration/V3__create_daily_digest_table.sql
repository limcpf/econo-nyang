-- Daily digest table for storing composed daily news digest
CREATE TABLE IF NOT EXISTS daily_digest (
    id BIGSERIAL PRIMARY KEY,
    digest_date DATE NOT NULL UNIQUE,
    title TEXT NOT NULL,
    body_markdown TEXT NOT NULL,
    total_articles INTEGER DEFAULT 0,
    total_summaries INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for performance
CREATE INDEX idx_daily_digest_date ON daily_digest(digest_date DESC);
CREATE INDEX idx_daily_digest_created_at ON daily_digest(created_at);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_daily_digest_updated_at 
    BEFORE UPDATE ON daily_digest 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE daily_digest IS '일일 경제뉴스 다이제스트 최종 결과물';
COMMENT ON COLUMN daily_digest.digest_date IS '다이제스트 대상 날짜 (YYYY-MM-DD)';
COMMENT ON COLUMN daily_digest.title IS '다이제스트 제목';
COMMENT ON COLUMN daily_digest.body_markdown IS '디스코드 발송용 마크다운 본문';
COMMENT ON COLUMN daily_digest.total_articles IS '처리된 총 기사 수';
COMMENT ON COLUMN daily_digest.total_summaries IS '생성된 총 요약 수';
COMMENT ON COLUMN daily_digest.created_at IS '다이제스트 생성 시간';
COMMENT ON COLUMN daily_digest.updated_at IS '다이제스트 최종 수정 시간';