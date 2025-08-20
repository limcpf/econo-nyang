-- Enable trigram extension for fuzzy text matching (must come first)
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

-- ============================
-- ARTICLES 인덱스
-- ============================

-- (source, published_at) 복합 인덱스: 소스/발행일 기반 조회
CREATE INDEX IF NOT EXISTS idx_articles_source_published_at
    ON articles (source, published_at DESC);

-- published_at 정렬 + created_at 커버링
-- 범위/정렬 최적화에 유리하며 중복 인덱스 피함
CREATE INDEX IF NOT EXISTS idx_articles_published_at_desc_inc_created_at
    ON articles (published_at DESC) INCLUDE (created_at);

-- created_at 범위 스캔용 (최근 N일 조회 시 사용)
-- 기존의 CURRENT_DATE 포함 부분 인덱스를 일반 인덱스로 대체
CREATE INDEX IF NOT EXISTS idx_articles_created_at_desc
    ON articles (created_at DESC);

-- 제목 트라이그램 인덱스: 중복/유사 제목 탐지
-- NULL 제목은 제외해 공간 절약 (부분 인덱스지만 함수 사용 아님 → 안전)
CREATE INDEX IF NOT EXISTS idx_articles_title_trgm
    ON articles USING gin (title gin_trgm_ops)
    WHERE title IS NOT NULL;

-- ============================
-- SUMMARIES 인덱스
-- ============================

-- 점수 정렬 + created_at 커버링
CREATE INDEX IF NOT EXISTS idx_summaries_score_desc_inc_created_at
    ON summaries (score DESC) INCLUDE (created_at);

-- 기사별 요약을 최신순으로 찾을 때
CREATE INDEX IF NOT EXISTS idx_summaries_article_created_at_desc
    ON summaries (article_id, created_at DESC);

-- created_at 범위 스캔용 (최근 N일 조회 시 사용)
CREATE INDEX IF NOT EXISTS idx_summaries_created_at_desc
    ON summaries (created_at DESC);

-- ============================
-- 제약조건 (Constraints)
-- ============================
DO $$
BEGIN
ALTER TABLE dispatch_log
    ADD CONSTRAINT chk_dispatch_log_status
        CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING', 'RETRY'));
EXCEPTION
    WHEN duplicate_object THEN
        NULL; -- 이미 존재하면 무시
END $$;

DO $$
BEGIN
ALTER TABLE dispatch_log
    ADD CONSTRAINT chk_dispatch_log_channel
    CHECK (channel IN ('discord', 'slack', 'webhook', 'email'));
EXCEPTION
    WHEN duplicate_object THEN
        NULL; -- 이미 존재하면 무시
END $$;



ALTER TABLE dispatch_log
    ADD CONSTRAINT chk_dispatch_log_attempt_count
        CHECK (attempt_count > 0);

ALTER TABLE summaries
    ADD CONSTRAINT chk_summaries_score
    CHECK (score >= 0 AND score <= 100);

COMMENT ON CONSTRAINT chk_dispatch_log_status ON dispatch_log IS 'Valid status values only';
COMMENT ON CONSTRAINT chk_dispatch_log_channel ON dispatch_log IS 'Valid channel types only';
COMMENT ON CONSTRAINT chk_dispatch_log_attempt_count ON dispatch_log IS 'Attempt count must be positive';
COMMENT ON CONSTRAINT chk_summaries_score ON summaries IS 'Score must be between 0 and 100';
