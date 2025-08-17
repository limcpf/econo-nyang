-- Dispatch log table for tracking message delivery status
CREATE TABLE IF NOT EXISTS dispatch_log (
    id BIGSERIAL PRIMARY KEY,
    digest_id BIGINT NOT NULL REFERENCES daily_digest(id) ON DELETE CASCADE,
    channel TEXT NOT NULL,         -- 'discord', 'slack', etc.
    webhook_ref TEXT,              -- webhook URL reference or identifier
    status TEXT NOT NULL,          -- 'SUCCESS', 'FAILED', 'PENDING', 'RETRY'
    response_snippet TEXT,         -- 응답 내용 일부 (디버깅용)
    error_message TEXT,            -- 실패 시 에러 메시지
    attempt_count INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for performance
CREATE INDEX idx_dispatch_log_digest_id ON dispatch_log(digest_id);
CREATE INDEX idx_dispatch_log_channel ON dispatch_log(channel);
CREATE INDEX idx_dispatch_log_status ON dispatch_log(status);
CREATE INDEX idx_dispatch_log_created_at ON dispatch_log(created_at);

-- Partial index for failed dispatches (for monitoring)
CREATE INDEX idx_dispatch_log_failed ON dispatch_log(digest_id, created_at) 
    WHERE status = 'FAILED';

-- Comments for documentation
COMMENT ON TABLE dispatch_log IS '다이제스트 발송 로그 및 상태 추적';
COMMENT ON COLUMN dispatch_log.digest_id IS '발송된 다이제스트 ID';
COMMENT ON COLUMN dispatch_log.channel IS '발송 채널 (discord, slack 등)';
COMMENT ON COLUMN dispatch_log.webhook_ref IS 'Webhook URL 참조 (보안상 마스킹)';
COMMENT ON COLUMN dispatch_log.status IS '발송 상태 (SUCCESS/FAILED/PENDING/RETRY)';
COMMENT ON COLUMN dispatch_log.response_snippet IS 'API 응답 내용 일부';
COMMENT ON COLUMN dispatch_log.error_message IS '실패 시 상세 에러 메시지';
COMMENT ON COLUMN dispatch_log.attempt_count IS '재시도 횟수';
COMMENT ON COLUMN dispatch_log.created_at IS '발송 시도 시간';