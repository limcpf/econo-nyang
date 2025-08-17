좋아. 아래는 **Java 8 + Spring Boot 2.7.x + Spring Batch 4.3.x + Jenkins** 기준의 \*\*상세 설계서(코드 최소)\*\*다. OpenAI API(요약·해설) 사용을 전제로 **비용/성능 최적화**와 **프롬프팅/배치 전략**을 포함했다. (모델/요금/기능은 공식 문서 기준으로 운영—**Structured Outputs**, **Batch API**, **Rate Limits** 등 참고함. ([OpenAI 플랫폼][1], [OpenAI][2], [OpenAI 요리책][3]))

# 1. 전체 아키텍처(개요)

* **배치 구동**: Jenkins 스케줄(매일 07:30 KST) → `java -jar` 실행 (톰캣 불필요)
* **Spring Batch Job**: `ECON_DAILY_DIGEST`

    * S1\_FETCH(수집) → S2\_EXTRACT(추출/정제) → **S3\_SUMMARIZE\_AI(LLM 요약/해설)** → S4\_RANK\_COMPOSE(선별/조립) → S5\_DISPATCH(발송)
* **DB**: PostgreSQL (기사/요약/다이제스트/발송로그)
* **OpenAI 연동**:

    * **Structured Outputs**로 JSON 스키마 강제(형식 안정성) ([OpenAI 플랫폼][1], [OpenAI][2])
    * **Batch API**로 새벽(03:00 KST) 비동기 일괄 처리 + 07:10 폴백(동기) ([OpenAI 플랫폼][4])
    * **Rate limits** 준수 + 백오프/동시성 제어 ([OpenAI 플랫폼][5], [OpenAI 요리책][3])

---

# 2. 모듈/패키지 및 주요 파일

```
/src/main/java
  └─ com.yourco.econdigest
     ├─ batch/config/           # 배치/스케줄/잡 파라미터
     ├─ batch/steps/            # Step 구현체(Reader/Processor/Writer/Tasklet)
     ├─ core/openai/            # OpenAI 클라이언트, 요청/응답 DTO, Batch 핸들러
     ├─ core/extract/           # 본문 추출기(허용 범위), 문장분할/Top-k
     ├─ core/rank/              # 중요도 산정, 중복 제거
     ├─ core/discord/           # 디스코드 전송 어댑터
     ├─ domain/                 # 엔티티/리포지토리(직접 드라이버 또는 JDBI/JdbcTemplate)
     └─ util/                   # 공통(시간대, 트렁케이션, 해시)
/src/main/resources
  ├─ application.yml            # 프로필/DB/LLM/타임아웃
  ├─ rss-sources.yml            # 소스/가중치/태그
  ├─ digest-template.yml        # 문장 템플릿/길이 제한
  └─ db/migration/              # Flyway 스크립트
Jenkinsfile
```

---

# 3. 배치 Job/Step 설계

## 3.1 Job 시그니처

* **Job 이름**: `ECON_DAILY_DIGEST`
* **JobParameters**

    * `targetDate`(기본=어제, KST 기준 `YYYY-MM-DD`)
    * `maxArticles`(기본 5\~8)
    * `dryRun`(true/false)
    * `useLLM`(default=true)
    * `llmModelSmall`, `llmModelMain` (스테이징/프로덕 구분)
    * `skipExtract/skipSummarize/skipDispatch` (선택적 스텝 스킵)
    * `webhookOverride`(옵션)

> **Batch 4.x/Boot 2.7**에선 `JobBuilderFactory/StepBuilderFactory` 사용이 일반적. (Boot3+ 권장 방식과 다름)

## 3.2 Step I/O & 예외·스킵

| Step                  | 입력                | 처리                                                                              | 출력/키                                              | 실패/스킵                                              | 멱등성                  |
| --------------------- | ----------------- | ------------------------------------------------------------------------------- | ------------------------------------------------- | -------------------------------------------------- | -------------------- |
| **S1\_FETCH**         | `rss-sources.yml` | RSS 수집, (title/url/published\_at/source) 리스트                                    | 후보 기사 캐시(메모리/임시테이블)                               | 소스별 타임아웃/재시도, 실패 소스만 경고                            | URL 해시로 중복 제거        |
| **S2\_EXTRACT**       | 후보 기사             | 허용 범위 내 본문 추출 → 문장분할 → 유사중복 제거 → Top-k 추출                                       | `ARTICLES` upsert(`url` unique), `raw_excerpt` 저장 | 추출 실패는 **Skip**(헤드라인 폴백 태그)                        | `url` UNIQUE         |
| **S3\_SUMMARIZE\_AI** | 기사 본문 or 헤드라인     | **Structured Outputs**로 요약/해설/불릿/용어 JSON 생성. 새벽 Batch 결과 우선 사용, 미존재 시 동기 API 호출 | `SUMMARIES` insert(기사별 1개)                        | LLM 에러(429/5xx)는 3회 재시도+지수 백오프, 스키마 불일치 시 1회 리프롬프트 | `article_id`+`model` |
| **S4\_RANK\_COMPOSE** | 기사+요약             | 출처 가중치+키워드 매칭+중복 등장량 → 점수화 → 상위 N 선별 → 디스코드 임베드용 텍스트 조립                         | `DAILY_DIGEST` upsert(`digest_date` unique)       | 조립 실패 시 안전 트렁케이션                                   | `digest_date` UNIQUE |
| **S5\_DISPATCH**      | 다이제스트             | 디스코드 Webhook 전송(임베드 N개), 응답 요약 저장                                               | `DISPATCH_LOG`                                    | HTTP 실패 시 3회 재시도, 최종 실패 시 경고 알림                    | 안전 재발송 가능            |

---

# 4. 데이터 모델(Flyway 마이그레이션)

* **파일명**

    * `db/migration/V1__articles.sql`
    * `db/migration/V2__summaries.sql`
    * `db/migration/V3__daily_digest.sql`
    * `db/migration/V4__dispatch_log.sql`

* **DDL 요지(필요 부분만)**

```sql
-- V1__articles.sql
CREATE TABLE IF NOT EXISTS articles(
  id BIGSERIAL PRIMARY KEY,
  source TEXT NOT NULL,
  url TEXT NOT NULL UNIQUE,
  title TEXT NOT NULL,
  published_at TIMESTAMPTZ,
  author TEXT,
  raw_excerpt TEXT,              -- 전문 대신 요약 추출물(저작권/보관 최소화)
  created_at TIMESTAMPTZ DEFAULT now()
);

-- V2__summaries.sql
CREATE TABLE IF NOT EXISTS summaries(
  id BIGSERIAL PRIMARY KEY,
  article_id BIGINT REFERENCES articles(id) ON DELETE CASCADE,
  model TEXT NOT NULL,
  summary_text TEXT NOT NULL,
  why_it_matters TEXT NOT NULL,
  bullets TEXT[] NOT NULL,
  glossary JSONB,                -- [{term, definition}]
  evidence_idx INT[],            -- 선택: 문장 인덱스
  score NUMERIC,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- V3__daily_digest.sql
CREATE TABLE IF NOT EXISTS daily_digest(
  id BIGSERIAL PRIMARY KEY,
  digest_date DATE NOT NULL UNIQUE,
  title TEXT NOT NULL,
  body_markdown TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- V4__dispatch_log.sql
CREATE TABLE IF NOT EXISTS dispatch_log(
  id BIGSERIAL PRIMARY KEY,
  digest_id BIGINT REFERENCES daily_digest(id) ON DELETE CASCADE,
  channel TEXT NOT NULL,         -- 'discord'
  webhook_ref TEXT,
  status TEXT NOT NULL,          -- SUCCESS | FAILED
  response_snippet TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);
```

보존 정책: `raw_excerpt` 30~~90일, 나머지 1~~2년(조직 정책에 따름).

---

# 5. OpenAI 연동(모델/요청/비용 최적화)

## 5.1 모델 운용(2단 라우팅)

* **Stage-A(대다수 기사)**: **소형/비용효율 모델**로 **사실 중심 요약(JSON 스키마 강제)**
* **Stage-B(상위 N건)**: **플래그십 모델**로 문체/가독성 리라이팅(추가 사실 생성 금지)
  → 모델 가용성·특성은 **공식 모델 목록** 확인 후 운영 변수로 관리. ([OpenAI 플랫폼][6])

## 5.2 Structured Outputs(JSON 스키마 강제)

* 목적: LLM 응답을 **정확히 스키마 준수**하도록 보장 → 파싱/실패율 저감. ([OpenAI 플랫폼][1], [OpenAI][2])

**요약 스키마(요지)** — `src/main/resources/llm/summary.schema.json`

```json
{
  "type":"object",
  "properties":{
    "summary":{"type":"string"},
    "why_it_matters":{"type":"string"},
    "bullets":{"type":"array","items":{"type":"string"},"minItems":3,"maxItems":5},
    "glossary":{"type":"array","items":{"type":"object","properties":{
      "term":{"type":"string"},
      "definition":{"type":"string"}
    }}}
  },
  "required":["summary","why_it_matters","bullets"]
}
```

## 5.3 Batch API(새벽 선요약 → 아침 폴백)

* **03:00 KST**: 전일 기사 요청을 **Batch API**에 일괄 등록(JSONL 업로드 → Batch 생성)
* **07:10 KST**: 완료 시 결과 사용, 미완료/실패 시 동기 API로 폴백
* 효과: 많은 요청을 **비동기로 처리**하고 **레이트리밋/비용 효율** 개선. ([OpenAI 플랫폼][4])

## 5.4 Rate limits/에러 처리

* 에러코드 429/5xx → **지수 백오프**(예: 1s→2s→4s), **동시성 제한**(소형=\~5, 메인=\~2)
* 공식 가이드라인에 따라 **버스트/컨텍스트 길이**도 제한 관리. ([OpenAI 플랫폼][5], [OpenAI Help Center][7])

---

# 6. 프롬프팅 전략(간결본)

## 6.1 System Prompt(요지) — `src/main/resources/llm/system.txt`

* 역할: “한국어 경제 해설가”
* 원칙: **사실 기반**, 허용 텍스트 밖 추정 금지, **투자 권유 금지**
* 스타일: 짧고 쉬운 문장, 한국 독자 관점(코스피/원화/금리/수출입)

## 6.2 User Prompt(구성)

* 메타: `title`, `source`, `published_at(KST)`, `url`, `country_hint=KR`
* 본문: **Top-k 핵심문장**(유사중복 제거), 3\~5천 토큰 이내
* 태스크:

    1. **요약(3\~5문장)**
    2. **왜 중요한가(2\~3문장)**
    3. **핵심 포인트(불릿 3\~5)**
    4. **용어풀이(1\~3)**
    5. (선택) **근거 문장 인덱스** 배열

## 6.3 Response Format (Structured Outputs)

* **response\_format**에 `json_schema` 지정(스키마=위 파일) → 항상 JSON 준수. ([OpenAI 플랫폼][1])

---

# 7. 설정 파일

## 7.1 `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASS}
  jackson:
    serialization:
      write-dates-as-timestamps: false

app:
  timezone: Asia/Seoul
  digest:
    sendAt: "07:30"
    maxArticles: 6
  openai:
    apiBase: "https://api.openai.com/v1"
    apiKey: ${OPENAI_API_KEY}
    modelSmall: ${OPENAI_MODEL_SMALL}   # ex) gpt-5-mini (운영 변수)
    modelMain: ${OPENAI_MODEL_MAIN}     # ex) gpt-5
    requestTimeoutSec: 30
    enableBatch: true
    batchCutoff: "07:10"                # Batch 완료 제한
    maxInputTokens: 5000
    maxOutputTokens: 900
```

## 7.2 `src/main/resources/rss-sources.yml` (예시)

```yaml
sources:
  - id: google_kr_econ
    url: "https://news.google.com/rss/search?q=경제 when:1d&hl=ko&gl=KR&ceid=KR:ko"
    weight: 1.0
  - id: hankyung_econ
    url: "https://www.hankyung.com/feed/economy"
    weight: 1.2
  # ...
policy:
  request:
    timeoutMs: 7000
    minIntervalMs: 400
```

## 7.3 `src/main/resources/digest-template.yml`

```yaml
discord:
  title: "어제의 경제 한눈에 (%s)"  # %s = YYYY-MM-DD
  embedLimit: 6
  truncate:
    title: 200
    description: 1800
  sections:
    order: [summary, why, bullets, glossary]
    labels:
      summary: "요약"
      why: "왜 중요?"
      bullets: "핵심"
      glossary: "용어풀이"
```

---

# 8. Jenkins 파이프라인

## 8.1 `Jenkinsfile` (핵심만)

```groovy
pipeline {
  agent any
  parameters {
    string(name: 'TARGET_DATE', defaultValue: '', description: 'YYYY-MM-DD, 비우면 어제(KST)')
    string(name: 'MAX_ARTICLES', defaultValue: '6', description: '상위 N')
    booleanParam(name: 'USE_LLM', defaultValue: true, description: 'AI 요약 사용')
    booleanParam(name: 'DRY_RUN', defaultValue: false, description: '발송 미수행')
  }
  environment {
    OPENAI_API_KEY = credentials('OPENAI_API_KEY')
    DB_URL = credentials('DB_URL')
    DB_USER = credentials('DB_USER')
    DB_PASS = credentials('DB_PASS')
  }
  triggers { cron('H 22 * * *') } // UTC 22 ~= KST 07
  stages {
    stage('Build') { steps { sh './mvnw -q -DskipTests package' } }
    stage('Migrate') { steps { sh 'java -jar target/app.jar --spring.flyway.migrate' } }
    stage('Run Job') {
      steps {
        sh """
        java -jar target/app.jar \
          --job.name=ECON_DAILY_DIGEST \
          targetDate=${params.TARGET_DATE} \
          maxArticles=${params.MAX_ARTICLES} \
          useLLM=${params.USE_LLM} \
          dryRun=${params.DRY_RUN}
        """
      }
    }
  }
  post {
    failure { echo '배치 실패: 로그/아티팩트 확인' }
  }
}
```

---

# 9. OpenAI 호출 설계(간단 코드 예시만)

## 9.1 동기 호출(Responses API + Structured Outputs) ([OpenAI 플랫폼][8])

`src/main/java/.../core/openai/OpenAiClient.java` (요지)

```java
// WebClient 또는 OkHttp 사용 예시(요지)
RequestBody body = RequestBody.create(MediaType.parse("application/json"),
  "{ \"model\": \""+model+"\", " +
  "  \"input\": "+jsonEscape(userPrompt)+","+
  "  \"response_format\": {\"type\":\"json_schema\",\"json_schema\": "+schemaJson+"},"+
  "  \"max_output_tokens\": 900 }");

Request req = new Request.Builder()
  .url(apiBase + "/responses")
  .header("Authorization", "Bearer " + apiKey)
  .post(body).build();
```

## 9.2 Batch API(요청 JSONL 생성/업로드/결과 폴링) ([OpenAI 플랫폼][4])

* `files.create`(JSONL 업로드) → `batches.create`(endpoint=`/v1/responses`)
* 상태 `completed`면 `batches.responses`로 결과 수신 → 매핑 저장
* 07:10까지 미완료면 **동기 폴백**

---

# 10. 길이 제한/트렁케이션 규칙

* **입력**: 문장 Top-k(가중치: 제목 키워드·숫자 포함·원저 인용)로 **\~5k tokens** 제한
* **출력**: 요약+왜중요+불릿+용어풀이 합산 **\~900 tokens**
* **디스코드**: 템플릿의 각 필드 별 **보수적 절삭**(title 200, desc 1800)

---

# 11. 중요도 산정(랭킹) 점수식(요지)

* `score = w_source + w_keywords + w_crossdup + w_recency + w_extractOk`

    * `w_source`: 출처 가중치
    * `w_keywords`: 금리/물가/환율/성장/부동산 등
    * `w_crossdup`: 다수 소스 교차 등장
    * `w_recency`: 전일 내 timestamp 가중
    * `w_extractOk`: 본문 추출 성공 보너스

---

# 12. 관측성/모니터링

* **로그**: Step별 처리/스킵/실패 수, OpenAI 응답 코드·지연(ms)
* **메트릭**: 소요시간, 성공률, 추출 성공률, LLM 호출 평균 토큰/비용 추정
* **알림**: Jenkins 실패/성공, 재시도 초과

---

# 13. 보안/컴플라이언스

* **OPENAI\_API\_KEY** 등 비밀은 Jenkins Credentials로만 주입(로그/아티팩트 금지)
* **저작권/TOS**: 전문 저장 대신 `raw_excerpt`(요지)로 제한, 보관기간 단축
* **PII 미수집**, 외부 응답은 상태코드/스니펫만 저장

---

# 14. 테스트 전략

* **단위**:

    * 추출기(고정 샘플 → Top-k 결과 스냅샷)
    * LLM 스키마 검증(Structured Outputs 결과 JSON Schema validate) ([OpenAI 플랫폼][1])
    * 랭킹 로직(정해진 입력 대비 상위 N 일치)
* **통합**:

    * 실제 RSS 일부 + LLM 스테이징 키 + 디스코드 테스트 Webhook
    * Batch 경로: JSONL 생성→업로드→Batch 완료→매핑 저장
* **부하**: 800건/일 입력, 95p 처리시간 측정
* **UAT 기준**: 핵심 누락 없음, 과장·권유 표현 0건

---

# 15. 재처리/런북(요지)

* **특정 날짜 재처리**: Jenkins 파라미터 `targetDate=YYYY-MM-DD`
* **단계별 재실행**: `skipExtract=true` 등 스위치로 부분 재실행
* **LLM만 재생성**: summaries 삭제 후 S3만 재실행(or `forceResummarize=true`)
* **발송 재시도**: `S5_DISPATCH`만 재수행(다이제스트 idempotent)

---

# 16. 성능·비용 최적화 체크리스트

* 소스 필터링(중복/저품질 제외)
* 입력 문장 Top-k/중복 제거 → **입력 토큰 축소**
* 새벽 **Batch API 선요약**(가능하면 최대한) ([OpenAI 플랫폼][4])
* 소형 모델 우선, 상위 N만 플래그십
* 실패 시 **지수 백오프 + 동시성 제한**(공식 가이드 참조) ([OpenAI 플랫폼][5], [OpenAI 요리책][3])

---

## 부록 A. 디스코드 메시지(임베드) 조립 규칙(요지)

* 제목: 기사 제목(절삭)
* 설명:

    * `요약:` 3\~5문장
    * `왜 중요?:` 1\~2문장
    * `핵심:` 불릿 3\~5
    * `용어풀이:` 1\~3(“용어=한 줄 정의”)
* 임베드 최대 N(기본 6), 과다 시 2건으로 분할(스팸 방지)

## 부록 B. 모델/문서 레퍼런스

* **Structured Outputs**: JSON 스키마 강제(형식 보증) ([OpenAI 플랫폼][1], [OpenAI][2])
* **Batch API**: 비동기 대량 처리(요청 업로드/상태 폴링/결과 조회) ([OpenAI 플랫폼][4])
* **Rate Limits**: 한도/버스트/대응 가이드, Cookbook 재시도 예시 ([OpenAI 플랫폼][5], [OpenAI 요리책][3], [OpenAI Help Center][7])
* **모델 목록/특성**: 최신 가용 모델과 파라미터 확인 후 운영 변수화 ([OpenAI 플랫폼][6])

---

원하면 여기서 **Step별 상세 I/O 표(필드 레벨), Jenkins 파라미터 사양서(yaml), LLM 요청/응답 샘플(json), 실패 케이스별 리프롬프트 문구**까지 바로 더 자세히 풀어줄게.

[1]: https://platform.openai.com/docs/guides/structured-outputs?utm_source=chatgpt.com "Structured model outputs - OpenAI API"
[2]: https://openai.com/index/introducing-structured-outputs-in-the-api/?utm_source=chatgpt.com "Introducing Structured Outputs in the API"
[3]: https://cookbook.openai.com/examples/how_to_handle_rate_limits?utm_source=chatgpt.com "How to handle rate limits | OpenAI Cookbook"
[4]: https://platform.openai.com/docs/guides/batch?utm_source=chatgpt.com "Batch API"
[5]: https://platform.openai.com/docs/guides/rate-limits?utm_source=chatgpt.com "Rate limits - OpenAI API"
[6]: https://platform.openai.com/docs/models?utm_source=chatgpt.com "Models - OpenAI API"
[7]: https://help.openai.com/en/articles/6891753-what-are-the-best-practices-for-managing-my-rate-limits-in-the-api?utm_source=chatgpt.com "What are the best practices for managing my rate limits in the API?"
[8]: https://platform.openai.com/docs/api-reference?utm_source=chatgpt.com "API Reference"
