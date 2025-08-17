-- Additional indexes and constraints for performance optimization

-- Composite indexes for common query patterns
CREATE INDEX idx_articles_source_published_at ON articles(source, published_at DESC);
CREATE INDEX idx_articles_published_at_created_at ON articles(published_at DESC, created_at DESC);

-- Index for text search on article titles (for duplicate detection)
CREATE INDEX idx_articles_title_trgm ON articles USING gin(title gin_trgm_ops);

-- Enable trigram extension for fuzzy text matching (if not exists)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Composite index for summaries ranking queries
CREATE INDEX idx_summaries_score_created_at ON summaries(score DESC, created_at DESC);

-- Index for finding summaries by date range
CREATE INDEX idx_summaries_article_created_at ON summaries(article_id, created_at DESC);

-- Partial indexes for active/recent data
CREATE INDEX idx_articles_recent ON articles(created_at DESC) 
    WHERE created_at >= (CURRENT_DATE - INTERVAL '30 days');

CREATE INDEX idx_summaries_recent ON summaries(created_at DESC) 
    WHERE created_at >= (CURRENT_DATE - INTERVAL '30 days');

-- Constraint to ensure valid status values in dispatch_log
ALTER TABLE dispatch_log 
ADD CONSTRAINT chk_dispatch_log_status 
CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING', 'RETRY'));

-- Constraint to ensure valid channel values
ALTER TABLE dispatch_log 
ADD CONSTRAINT chk_dispatch_log_channel 
CHECK (channel IN ('discord', 'slack', 'webhook', 'email'));

-- Constraint to ensure positive attempt count
ALTER TABLE dispatch_log 
ADD CONSTRAINT chk_dispatch_log_attempt_count 
CHECK (attempt_count > 0);

-- Constraint to ensure valid score range in summaries
ALTER TABLE summaries 
ADD CONSTRAINT chk_summaries_score 
CHECK (score >= 0 AND score <= 100);

-- Comments for new constraints
COMMENT ON CONSTRAINT chk_dispatch_log_status ON dispatch_log IS 'Valid status values only';
COMMENT ON CONSTRAINT chk_dispatch_log_channel ON dispatch_log IS 'Valid channel types only';
COMMENT ON CONSTRAINT chk_dispatch_log_attempt_count ON dispatch_log IS 'Attempt count must be positive';
COMMENT ON CONSTRAINT chk_summaries_score ON summaries IS 'Score must be between 0 and 100';