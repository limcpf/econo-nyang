## 1) 목적과 범위

* **목적**: 누구나 읽기 쉬운 코드/문서, **TDD 기반**의 안정적인 배치 개발, **Claude Code** 보조를 활용한 생산성 향상을 표준화한다.
* **적용 범위**: 배치 애플리케이션(수집/추출/요약/조립/발송), DB 마이그레이션, Jenkins 파이프라인 스크립트, 운영 문서.

---

## 2) 핵심 원칙

* **TDD 사이클(Red→Green→Refactor)**: 테스트 선작성 → 최소 구현 → 리팩터.
* **Clean Code**: 짧고 명확하게. 한 함수/클래스는 한 가지 일. 축약어·암시 금지.
* **KISS/DRY**: 단순하게, 중복 제거. 복잡도 상승 시 분리·추상화.
* **명시적 실패**: 모호한 반환(null) 대신 예외/옵션(필요 시)을 사용하고, **멱등성** 보장.
* **문서 우선**: 결정·가정·트레이드오프는 `/docs`에 남긴다(ADR/Runbook/학습 로그).

---

## 3) 디렉터리 & 파일 규칙

```
/src/main/java/com/yourco/econdigest/...
/src/test/java/com/yourco/econdigest/...
/src/main/resources/
  application.yml, rss-sources.yml, digest-template.yml
/db/migration/                    # Flyway
/docs/
  adr/                            # Architecture Decision Record
  specs/                          # PRD/상세설계/파라미터 사양 등
  runbooks/                       # 재처리/장애 대응
  learn/                          # TIL, 회고, 실험 기록
  checklists/                     # 리뷰/릴리즈 체크리스트
CLAUDE.md
```

* **문서 파일명**: `YYYY-MM-DD--topic.md` (예: `2025-08-17--llm-routing-design.md`)
* **마이그레이션**: `V{번호}__{스네이크케이스}.sql` (예: `V1__create_articles.sql`)

---

## 4) 네이밍 규칙

* **패키지**: `com.yourco.econdigest.{layer}` (e.g., `batch.steps`, `core.openai`)
* **배치 명칭**

    * Job: `ECON_DAILY_DIGEST`, `ECON_BACKFILL`
    * Step: `S1_FETCH`, `S2_EXTRACT`, `S3_SUMMARIZE_AI`, `S4_RANK_COMPOSE`, `S5_DISPATCH`
* **클래스**: `FetchRssTasklet`, `ExtractArticleProcessor`, `OpenAiClient`
* **메서드**: 동사 시작 `buildDigest()`, `rankArticles()`
* **상수**: `UPPER_SNAKE_CASE`
* **환경키**: `OPENAI_API_KEY`, `DB_URL`(비밀은 코드에 직접 쓰지 않는다)

---

## 5) 주석·Javadoc

* **왜(WHY)와 의도**를 적고, \*\*무엇(WHAT)\*\*은 코드로 드러나게 한다.
* **Javadoc**은 공개/공용 API에만. 내부 구현엔 과도한 주석 지양.
* 사용자의 선호에 따라 **`@return` 표기는 생략** 가능(필요 시에만).
* **이모지 금지**, 마케팅성 표현 금지.

---

## 6) 커밋·브랜치·PR

* **브랜치**: `feat/*`, `fix/*`, `chore/*`, `docs/*`, `refactor/*`
* **커밋 메시지(사용자 선호 반영)**

    * 형태: `feat: 한글(영어)` 또는 `fix: 한글(영어)`
    * 예: `feat: 전일 기사 요약 스키마 추가(Add summary JSON schema)`
* **PR 템플릿(`/docs/checklists/PR_TEMPLATE.md`)**

    * 목적/변경 요약/스코프/테스트 증빙(스크린샷·로그)/릴리스 노트/리스크·롤백
* 작은 PR(≤300라인) 권장. **하나의 PR = 하나의 주제**.

---

## 7) 테스트 전략(TDD)

* **우선순위**: 단위(Unit) → 통합(Slice/DB/HTTP) → E2E(Job end-to-end)
* **명명 규칙**

    * 테스트 클래스: `ClassNameTest`, `S3SummarizeAiStepIT`(IT=Integration Test)
    * 테스트 메서드: `given_*_when_*_then_*` 또는 `should_*`
* **테스트 픽스처**: `src/test/resources/fixtures/...` (샘플 RSS, 본문, LLM 응답)
* **커버리지**: 라인 80% 이상·핵심 경로 100% 목표(JaCoCo 리포트)
* **외부 연동**: LLM/Discord/HTTP는 기본 **Mock**. E2E는 스테이징 키/웹훅만.

> 예시(형식만, 구현 X)

```java
// S3SummarizeAiProcessorTest.java
// given 전처리된 본문과 모델 라우팅이 있을 때
// when 소형 모델로 요약을 생성하면
// then 스키마를 준수한 JSON이 반환되어야 한다(필수 필드 존재)
```

---

## 8) Spring Batch 컨벤션

* **JobParameters**는 모두 명시적(날짜·한도·토글). 재처리에 유리하게 설계.
* **Step 경계**는 I/O로 구분되고, 각 Step은 **멱등적**이어야 한다.

    * 예: `digest_date` UNIQUE, `articles.url` UNIQUE
* **실패 정책**: 아이템 단위 **Skip + 재시도(백오프)**, 임계치 초과 시 Step Fail.
* **로그**: 단계별 처리/스킵/실패 건수, 외부 호출 지연(ms), 응답코드.

---

## 9) 코드 스타일·정적 분석

* **포맷터**: Spotless + Google Java Format (팀 기본 설정 공유)
* **정적 분석**: Checkstyle 또는 SonarLint(무료), 규칙 위반 PR 차단(경미한 것은 주석 허용 태그로 예외)
* **메소드 길이/인자 수** 상한 정의(권장: 30라인/5개)

---

## 10) 설정·비밀·보안

* 환경 변수/크리덴셜은 **Jenkins Credentials**로 주입. 코드/로그/PR에 노출 금지.
* 외부 호출 타임아웃·재시도 정책은 `application.yml`에 명시.
* 샘플 설정은 `/docs/specs/`에 **마스킹** 상태로 예시 제공.

---

## 11) Claude Code 사용 가이드

* **프롬프트 패턴**

    * `ROLE`: “TDD 코치 & 코드 리뷰어”
    * `TASK`: “이 테스트를 통과시키는 최소 구현”
    * `CONSTRAINTS`: “Java 8, Spring Batch 4.3, 멱등성 유지, 사이드이펙트 금지”
    * `ACCEPTANCE CRITERIA`: “스키마 준수/커버리지/성능/로그”
* **워크플로우**

    1. **테스트부터 작성**(붉은 불) → Claude에 “테스트 통과 위한 최소 변경만” 요청
    2. **리팩터링 요청**: “가독성/성능/명명 개선, 사이드이펙트 금지”
    3. **문서화**: 완료 시 `/docs/learn/YYYY-MM-DD--{topic}.md`에 요약/비교/배운점 저장
* **금지/주의**

    * 비밀키/내부 URL 제공 금지, 실제 데이터 업로드 금지
    * 장황한 자동생성 코드 지양(“필요 최소”만 수용)

---

## 12) 문서화 규칙(`/docs`)

* **ADR**: 의사결정 1건당 1파일. 템플릿: 배경/옵션/결정/근거/영향/대안.
* **Runbook**: 재처리·장애 대응 절차(파라미터 예시, 실패 시나리오, 롤백).
* **Spec**: PRD/상세설계/파라미터 사양/스키마 JSON.
* **Learn**: 실험/회고/TIL. 실패도 기록(무엇을 시도했고 왜 접었는지).

---

## 13) 리뷰 체크리스트(발췌)

* 테스트 선행 여부 / 커버리지 / 경계·예외 케이스
* 명명·함수 길이 / 책임 분리 / 사이드이펙트
* Step 멱등성·재실행 안전 / 트랜잭션 범위
* 외부호출 타임아웃·재시도·로깅 / PII·비밀 키 노출 여부
* 문서(`/docs`) 갱신 여부(ADR/Spec/Runbook/Learn)

---

## 14) 릴리즈·품질 게이트

* **CI**: mvn test → JaCoCo 리포트 → 정적분석 → 패키징
* **품질 임계**: 테스트 실패 0, 커버리지 < 80% 시 차단, Checkstyle 오류 차단
* **릴리즈 노트**: 변경 요약/마이그레이션/런북 링크/리스크·롤백

---

## 15) 예시 작업 로그(학습용 템플릿)

`/docs/learn/2025-08-17--summarize-structured-outputs.md`

```
# 오늘의 학습/작업
- 목표: Structured Outputs로 요약 응답 스키마 강제
- 시도: 소형 모델 Stage-A → JSON 스키마 준수율 100% 확인
- 배운 점: 입력 문장 Top-k가 토큰 절약에 유효
- 다음: Stage-B 리라이팅 톤 가이드 개선
```

