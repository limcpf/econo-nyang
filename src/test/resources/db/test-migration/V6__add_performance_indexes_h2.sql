-- Additional performance indexes and constraints (H2 compatible)

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_articles_source_date ON articles(source, published_at);
CREATE INDEX IF NOT EXISTS idx_articles_date_source ON articles(published_at, source);

-- Composite indexes for summaries
CREATE INDEX IF NOT EXISTS idx_summaries_score_date ON summaries(score DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_summaries_article_model ON summaries(article_id, model);

-- Composite indexes for dispatch tracking
CREATE INDEX IF NOT EXISTS idx_dispatch_status_channel ON dispatch_log(status, channel);
CREATE INDEX IF NOT EXISTS idx_dispatch_digest_status ON dispatch_log(digest_id, status);

-- Additional constraints for data integrity
-- Note: H2 constraint syntax may differ slightly from PostgreSQL

-- Ensure score is within valid range
ALTER TABLE summaries ADD CONSTRAINT chk_summaries_score 
    CHECK (score IS NULL OR (score >= 0 AND score <= 100));

-- Ensure status values are valid
ALTER TABLE dispatch_log ADD CONSTRAINT chk_dispatch_status 
    CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING', 'RETRY'));

-- Ensure channel values are valid  
ALTER TABLE dispatch_log ADD CONSTRAINT chk_dispatch_channel
    CHECK (channel IN ('discord', 'slack', 'webhook', 'email', 'sms'));

-- Ensure attempt_count is positive
ALTER TABLE dispatch_log ADD CONSTRAINT chk_dispatch_attempts
    CHECK (attempt_count > 0);