-- Dispatch log table for tracking message delivery (H2 compatible)
CREATE TABLE IF NOT EXISTS dispatch_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    digest_id BIGINT NOT NULL,
    channel TEXT NOT NULL,         -- 'discord', 'slack', 등
    webhook_ref TEXT,              -- webhook URL의 마스킹된 참조
    status TEXT NOT NULL,          -- 'SUCCESS', 'FAILED', 'PENDING', 'RETRY'
    response_snippet TEXT,         -- API 응답의 일부 (디버깅용)
    error_message TEXT,            -- 실패 시 에러 메시지
    attempt_count INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (digest_id) REFERENCES daily_digest(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_dispatch_log_digest_id ON dispatch_log(digest_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_log_channel ON dispatch_log(channel);
CREATE INDEX IF NOT EXISTS idx_dispatch_log_status ON dispatch_log(status);
CREATE INDEX IF NOT EXISTS idx_dispatch_log_created_at ON dispatch_log(created_at);