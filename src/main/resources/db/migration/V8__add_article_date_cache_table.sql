-- 기사 날짜 추출 결과 캐시 테이블
CREATE TABLE article_date_cache (
    id BIGSERIAL PRIMARY KEY,
    url_hash VARCHAR(64) NOT NULL UNIQUE,
    source_name VARCHAR(100) NOT NULL,
    extracted_date TIMESTAMP NOT NULL,
    extraction_method VARCHAR(50) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    extraction_details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_verified_at TIMESTAMP,
    verification_count INTEGER NOT NULL DEFAULT 0,
    is_valid BOOLEAN NOT NULL DEFAULT true
);

-- 인덱스 생성
CREATE INDEX idx_url_hash ON article_date_cache(url_hash);
CREATE INDEX idx_source_created ON article_date_cache(source_name, created_at);
CREATE INDEX idx_extraction_method ON article_date_cache(extraction_method);
CREATE INDEX idx_confidence_valid ON article_date_cache(confidence_score, is_valid);
CREATE INDEX idx_created_at ON article_date_cache(created_at);

-- 코멘트 추가
COMMENT ON TABLE article_date_cache IS '기사 날짜 추출 결과 캐시 테이블';
COMMENT ON COLUMN article_date_cache.url_hash IS 'URL의 SHA-256 해시값';
COMMENT ON COLUMN article_date_cache.source_name IS '언론사명';
COMMENT ON COLUMN article_date_cache.extracted_date IS '추출된 발행일자';
COMMENT ON COLUMN article_date_cache.extraction_method IS '추출 방법 (url, meta, regex, pattern 등)';
COMMENT ON COLUMN article_date_cache.confidence_score IS '추출 신뢰도 (0.0~1.0)';
COMMENT ON COLUMN article_date_cache.extraction_details IS '추출 방법 상세 정보';
COMMENT ON COLUMN article_date_cache.verification_count IS '검증 횟수 (재사용 횟수)';
COMMENT ON COLUMN article_date_cache.is_valid IS '유효성 여부';