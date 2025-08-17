-- H2-compatible sample data for testing

-- Insert sample articles
INSERT INTO articles (source, url, title, published_at, author, raw_excerpt) VALUES
('hankyung', 'https://example.com/article1', '한국은행 기준금리 동결...경기 회복세 지속', 
 DATEADD('DAY', -1, CURRENT_TIMESTAMP), '김기자', 
 '한국은행이 기준금리를 현 수준에서 동결하기로 결정했다. 금융통화위원회는 경기 회복세가 지속되고 있다고 판단했다.'),

('google_kr_econ', 'https://example.com/article2', '코스피 2400선 회복...외국인 매수세 지속',
 DATEADD('HOUR', -2, CURRENT_TIMESTAMP), '박기자',
 '코스피가 2400선을 회복하며 상승세를 이어갔다. 외국인 투자자들의 매수세가 지속되고 있다.'),

('hankyung', 'https://example.com/article3', '원달러 환율 1300원대 중반...달러 약세 영향',
 DATEADD('HOUR', -3, CURRENT_TIMESTAMP), '이기자',
 '원달러 환율이 1300원대 중반에서 거래되고 있다. 미국 달러 약세가 주요 요인으로 분석된다.');

-- H2 uses comma-separated values instead of arrays for testing
INSERT INTO summaries (article_id, model, summary_text, why_it_matters, bullets, glossary, score) VALUES
(1, 'gpt-4o-mini', 
 '한국은행이 기준금리를 현 수준에서 동결했습니다. 금융통화위원회는 경기 회복세가 지속되고 있다고 판단했습니다.',
 '기준금리 동결은 대출금리와 예금금리에 직접적인 영향을 미치며, 부동산 시장과 소비에도 영향을 줍니다.',
 '기준금리 동결 결정,경기 회복세 지속 판단,금융시장 안정성 고려',
 '{"term": "기준금리", "definition": "중앙은행이 시중은행에 돈을 빌려줄 때 적용하는 기본 금리"}',
 85.5),

(2, 'gpt-4o-mini',
 '코스피가 2400선을 회복하며 상승세를 보였습니다. 외국인 투자자들의 지속적인 매수가 주요 동력이었습니다.',
 '코스피 상승은 국내 기업들의 시가총액 증가와 투자심리 개선을 의미합니다.',
 '코스피 2400선 회복,외국인 매수세 지속,투자심리 개선',
 '{"term": "코스피", "definition": "한국종합주가지수로 국내 주식시장의 대표 지수"}',
 78.2);

-- Insert sample daily digest
INSERT INTO daily_digest (digest_date, title, body_markdown, total_articles, total_summaries) VALUES
(DATEADD('DAY', -1, CURRENT_DATE), 
 '어제의 경제 한눈에 (2025-08-16)',
 '## 주요 경제 뉴스\n\n### 한국은행 기준금리 동결\n경기 회복세를 고려한 신중한 결정...\n\n### 코스피 2400선 회복\n외국인 매수세에 힘입어 상승...',
 3, 2);

-- Insert sample dispatch log
INSERT INTO dispatch_log (digest_id, channel, webhook_ref, status, response_snippet, attempt_count) VALUES
(1, 'discord', 'webhook_***masked***', 'SUCCESS', '{"status": "ok", "message_id": "123456"}', 1);