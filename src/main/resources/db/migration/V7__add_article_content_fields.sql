-- Article 테이블에 본문 추출 관련 필드 추가

-- 본문 내용 필드
ALTER TABLE articles ADD COLUMN IF NOT EXISTS content TEXT;

-- 본문 추출 완료 시간 필드  
ALTER TABLE articles ADD COLUMN IF NOT EXISTS extracted_at TIMESTAMP;

-- 본문 추출 에러 메시지 필드
ALTER TABLE articles ADD COLUMN IF NOT EXISTS extract_error VARCHAR(500);

-- 인덱스 추가 (본문이 있는 기사 빠른 조회용)
CREATE INDEX IF NOT EXISTS idx_articles_content_extracted 
ON articles(id) WHERE content IS NOT NULL;

-- 인덱스 추가 (추출 에러 발생한 기사 조회용)
CREATE INDEX IF NOT EXISTS idx_articles_extract_error 
ON articles(id) WHERE extract_error IS NOT NULL;