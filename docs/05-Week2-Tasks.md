# EconoNyang Week 2 개발 태스크

**문서명**: Week 2 실제 RSS 수집 및 Discord 발송 모드 전환 작업 태스크
**작성일**: 2025-08-21  
**목적**: `docs/04-Week2-실제모드-구현방법.md`를 바탕으로 구체적인 개발 태스크 계획 수립

---

## 📋 전체 개요

### 🎯 주요 목표
- 더미 모드 → 실제 RSS 수집/처리/발송 모드 전환
- ExecutionContext 직렬화 문제 해결
- 5단계 배치 파이프라인 실제 구현

### 📊 현재 상태  
- ✅ Spring Boot + Spring Batch 환경 구축 완료
- ✅ PostgreSQL DB 연결 및 스키마 구성 완료  
- ✅ 5단계 더미 배치 파이프라인 동작 확인
- ✅ **실제 데이터 처리 완전 구현** (2025-08-24 완료) 🎉
- ✅ **Phase 1, 2, 3 모두 완료** ⭐

---

## 🏆 Phase 1: 핵심 인프라 구축 (필수)

### Task 1.1: ExecutionContext 직렬화 문제 해결
**우선순위**: 🔴 Critical  
**예상 시간**: 2-3시간  
**담당자**: Dev

#### 서브태스크:
1. **ArticleService 기본 CRUD 구현**
   - `saveNewArticles()` - 중복 방지 저장
   - `findByIds()` - ID 리스트로 조회
   - `findExtractedByIds()` - 본문 추출된 기사만 조회

2. **ExecutionContext 저장 방식 변경**
   - 복잡한 객체 → 기본 데이터만 저장
   - ArticleDto 리스트 → articleIds 리스트
   - LocalDateTime → 기본 타입

#### 수락 기준:
- [x] 배치 실행 시 ExecutionContext 직렬화 오류 없음
- [x] Step 간 데이터 전달 정상 동작
- [x] 단위 테스트 통과 (커버리지 80% 이상)

**✅ COMPLETED - 2025-08-24**

---

### Task 1.2: RSS 피드 수집 서비스 구현 (S1_FETCH)
**우선순위**: 🔴 Critical  
**예상 시간**: 4-6시간  
**담당자**: Dev

#### 서브태스크:
1. **RssFeedService 기본 구현**
   - Rome/JDOM을 활용한 RSS 파싱
   - `rss-sources.yml` 설정 읽기
   - 타임아웃 및 재시도 로직

2. **키워드 필터링 구현**
   - includeKeywords OR 조건
   - excludeKeywords AND 조건  
   - 제목 길이 제한

3. **중복 제거 로직**
   - URL 기반 중복 제거
   - 제목 유사도 기반 중복 제거 (Optional)

#### 수락 기준:
- [x] 실제 RSS 소스에서 기사 수집 성공
- [x] 키워드 필터링 정상 동작
- [x] 중복 기사 제거 확인
- [x] 타임아웃/재시도 로직 동작
- [x] 처리 시간 15초 이내
- [x] **추가**: RSS별 시간 필터링 Strategy Pattern 구현 ✨

**✅ COMPLETED - 2025-08-24**

---

### Task 1.3: Discord Webhook 서비스 구현 (S5_DISPATCH)
**우선순위**: 🔴 Critical  
**예상 시간**: 3-4시간  
**담당자**: Dev

#### 서브태스크:
1. **DiscordService 기본 구현**
   - Webhook URL 환경변수 설정
   - HTTP 클라이언트 (RestTemplate/WebClient)
   - Embed 메시지 구조 설계

2. **메시지 분할 로직**
   - Discord 2000자 제한 대응
   - 마크다운 구조 유지하며 분할

3. **에러 처리 및 재시도**
   - Rate Limit 대응 (지수 백오프)
   - 발송 실패 시 로깅

#### 수락 기준:
- [x] Discord 채널에 메시지 발송 성공
- [x] 긴 메시지 자동 분할 동작
- [x] Rate Limit 상황 재시도 동작
- [x] 발송 성공/실패 로그 기록
- [x] DRY RUN 모드 동작

**✅ COMPLETED - 2025-08-24**

---

## 🥈 Phase 2: 콘텐츠 처리 구현 (중요)

### Task 2.1: 본문 추출 서비스 구현 (S2_EXTRACT)
**우선순위**: 🟡 High  
**예상 시간**: 5-7시간  
**담당자**: Dev

#### 서브태스크:
1. **ContentExtractionService 구현**
   - Jsoup를 활용한 HTML 파싱
   - 사이트별 본문 추출 규칙
   - User-Agent 순환 및 지연 처리

2. **병렬 처리 최적화**
   - parallelStream() 활용
   - 스레드풀 크기 조정
   - 타임아웃 관리

3. **에러 처리**
   - 추출 실패 시 에러 메시지 저장
   - 재시도 로직 (Optional)

#### 수락 기준:
- [x] 다양한 뉴스 사이트 본문 추출 성공
- [x] 병렬 처리로 성능 향상 확인
- [x] 추출 실패 기사 에러 로깅
- [x] 처리 시간 60초 이내 (10개 기사 기준)
- [x] S2_EXTRACT 단계와 통합 완료
- [x] 18개 단위 테스트 모두 통과 ✅

**✅ COMPLETED - 2025-08-24**

---

### Task 2.2: 다이제스트 템플릿 서비스 구현 (S4_RANK_COMPOSE)
**우선순위**: 🟡 High  
**예상 시간**: 4-5시간  
**담당자**: Dev

#### 서브태스크:
1. **DigestTemplateService 구현**
   - `digest-template.yml` 템플릿 로드
   - Mustache/Thymeleaf 템플릿 엔진 연동
   - 변수 치환 및 마크다운 생성

2. **DailyDigest 엔티티 처리**
   - Summary 목록을 마크다운으로 변환
   - 통계 정보 계산 및 삽입

3. **템플릿 종류별 처리**
   - default, simple, detailed 템플릿
   - 형식별 처리 (markdown, html)

#### 수락 기준:
- [x] 템플릿 기반 마크다운 다이제스트 생성
- [x] 변수 치환 정상 동작
- [x] 다양한 템플릿 형식 지원
- [x] DailyDigest DB 저장 성공
- [x] S4_RANK_COMPOSE 단계와 통합 완료
- [x] 10개 단위 테스트 모두 통과 ✅

**✅ COMPLETED - 2025-08-24**

---

## 🥉 Phase 3: AI 통합 및 고도화 (선택)

### Task 3.1: OpenAI API 요약 서비스 구현 (S3_SUMMARIZE_AI)
**우선순위**: 🟢 Medium  
**예상 시간**: 6-8시간  
**담당자**: Dev

#### 서브태스크:
1. **OpenAiClient 실제 연동**
   - Structured Outputs 스키마 적용
   - gpt-4o-mini 모델 사용
   - 비용 최적화 (토큰 제한)

2. **EconomicSummaryResponse 처리**
   - JSON 응답을 Summary 엔티티로 변환
   - 중요도 점수 및 bullets 배열 처리

3. **에러 처리 및 폴백**
   - API 실패 시 기본 요약 생성
   - 토큰 제한 초과 시 본문 자르기

#### 수락 기준:
- [x] OpenAI API 호출 및 응답 처리 성공
- [x] Structured Outputs JSON 스키마 준수
- [x] 중요도 점수 정확도 검증
- [x] API 비용 모니터링 구현
- [x] EconomicSummaryResponse → Summary 엔티티 변환
- [x] S3_SUMMARIZE_AI 단계와 통합 완료
- [x] 15개 단위 테스트 모두 통과 ✅

**✅ COMPLETED - 2025-08-24**

---

### Task 3.2: 고급 중요도 산정 알고리즘 (S4_RANK_COMPOSE 고도화)
**우선순위**: 🟢 Low  
**예상 시간**: 4-6시간  
**담당자**: Dev

#### 서브태스크:
1. **다차원 중요도 계산**
   - AI 점수 + 키워드 가중치
   - 뉴스 소스 신뢰도
   - 시간 가중치 (최신성)

2. **섹터별 분류 및 균형**
   - 경제일반, 증권, 부동산 등 섹터 분류
   - 섹터 간 균형 있는 선별

#### 수락 기준:
- [ ] 기존 Math.min() 대신 실제 알고리즘 적용
- [ ] 섹터 균형 및 다양성 확보
- [ ] 중요도 점수 정확도 향상

---

## 🛠️ Phase 4: 운영 및 모니터링

### Task 4.1: 에러 처리 및 로깅 강화
**우선순위**: 🟡 High  
**예상 시간**: 2-3시간  
**담당자**: Dev

#### 서브태스크:
1. **Step별 상세 로깅**
   - 처리 건수, 성공/실패 통계
   - 처리 시간 측정
   - 외부 API 호출 로그

2. **예외 상황 처리**
   - 네트워크 타임아웃
   - API 응답 오류
   - DB 트랜잭션 실패

#### 수락 기준:
- [ ] 각 Step별 상세 로그 출력
- [ ] 예외 상황 시 적절한 에러 메시지
- [ ] 배치 실행 통계 정보 제공

---

### Task 4.2: 테스트 코드 작성 및 품질 향상
**우선순위**: 🟡 High  
**예상 시간**: 6-8시간  
**담당자**: Dev

#### 서브태스크:
1. **단위 테스트 작성**
   - 각 Service 계층 테스트
   - Mock을 활용한 외부 의존성 격리
   - 경계값 및 예외 케이스 테스트

2. **통합 테스트 작성**
   - 배치 Job E2E 테스트
   - TestContainers를 활용한 DB 테스트
   - 실제 RSS 피드 연동 테스트 (선택)

#### 수락 기준:
- [ ] 라인 커버리지 80% 이상
- [ ] 핵심 비즈니스 로직 100% 커버리지
- [ ] CI/CD 파이프라인 통과

---

## 📅 개발 스케줄

### Week 2-1 (8/21-8/23): Phase 1 집중
- Day 1: Task 1.1 (ExecutionContext 해결)
- Day 2: Task 1.2 (RSS 수집 구현) 
- Day 3: Task 1.3 (Discord 서비스)

### Week 2-2 (8/24-8/27): Phase 2 진행
- Day 1-2: Task 2.1 (본문 추출)
- Day 3: Task 2.2 (템플릿 서비스)
- Day 4: 테스트 및 통합

### Week 3+ (8/28~): Phase 3 & 4 (선택사항)
- OpenAI 통합 및 고도화 기능
- 운영 모니터링 및 품질 향상

---

## ⚡ Quick Start 가이드

### 개발 환경 준비
```bash
# PostgreSQL 시작
docker-compose up -d postgres

# 환경변수 설정
cp .env.example .env
# DISCORD_WEBHOOK_URL, OPENAI_API_KEY 설정

# 배치 테스트 실행
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--batch.auto-run=true --dryRun=true"
```

### 구현 우선순위
1. **ExecutionContext 문제 해결** → 배치 안정성 확보
2. **RSS 수집** → 실제 데이터 소스 확보  
3. **Discord 발송** → 결과물 확인 가능
4. **본문 추출** → 콘텐츠 품질 향상
5. **AI 요약** → 최종 목표 달성

---

## 📊 성공 지표

### 기술적 지표
- [ ] 배치 실행 성공률 95% 이상
- [ ] 전체 처리 시간 5분 이내 (10개 기사)
- [ ] 메모리 사용량 1GB 이하
- [ ] 테스트 커버리지 80% 이상

### 비즈니스 지표  
- [ ] RSS 수집 성공률 90% 이상
- [ ] 키워드 필터링 정확도 확인
- [ ] Discord 발송 성공률 98% 이상
- [ ] 다이제스트 품질 만족도 (수동 확인)

---

**다음 단계**: Task 1.1부터 순차적으로 진행하여 점진적으로 실제 모드로 전환