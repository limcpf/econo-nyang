-- Summaries table for storing AI-generated summaries and explanations
CREATE TABLE IF NOT EXISTS summaries (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    model TEXT NOT NULL,
    summary_text TEXT NOT NULL,
    why_it_matters TEXT NOT NULL,
    bullets TEXT[] NOT NULL,
    glossary JSONB,                -- [{term: string, definition: string}]
    evidence_idx INT[],            -- 선택: 문장 인덱스 배열
    score NUMERIC(10,4),           -- 중요도 점수
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for performance
CREATE INDEX idx_summaries_article_id ON summaries(article_id);
CREATE INDEX idx_summaries_model ON summaries(model);
CREATE INDEX idx_summaries_score ON summaries(score DESC);
CREATE INDEX idx_summaries_created_at ON summaries(created_at);

-- Unique constraint to prevent duplicate summaries for same article+model
CREATE UNIQUE INDEX idx_summaries_article_model_unique ON summaries(article_id, model);

-- Comments for documentation
COMMENT ON TABLE summaries IS 'AI 생성 기사 요약 및 해설 정보';
COMMENT ON COLUMN summaries.article_id IS '연관된 기사 ID';
COMMENT ON COLUMN summaries.model IS '사용된 AI 모델명 (예: gpt-4o-mini, gpt-4o)';
COMMENT ON COLUMN summaries.summary_text IS '3-5문장 요약';
COMMENT ON COLUMN summaries.why_it_matters IS '왜 중요한가 설명 (2-3문장)';
COMMENT ON COLUMN summaries.bullets IS '핵심 포인트 배열 (3-5개)';
COMMENT ON COLUMN summaries.glossary IS '용어풀이 JSON 배열';
COMMENT ON COLUMN summaries.evidence_idx IS '요약 근거가 된 문장 인덱스들';
COMMENT ON COLUMN summaries.score IS '중요도 점수 (높을수록 중요)';
COMMENT ON COLUMN summaries.created_at IS '요약 생성 시간';