# Week 1 완료 현황 및 개발환경 구성 가이드

## 📋 프로젝트 현재 상태

### 완료된 기능 (Tasks 1-10)
- ✅ Spring Boot 2.7.x + Spring Batch 4.3.x 프로젝트 구조
- ✅ PostgreSQL 데이터베이스 스키마 및 Flyway 마이그레이션  
- ✅ RSS 피드 수집 배치 처리 시스템
- ✅ 웹 스크래핑을 통한 본문 추출 로직
- ✅ OpenAI API 클라이언트 (Structured Outputs 지원)
- ✅ AI 요약/해설 생성 배치 스텝
- ✅ YAML 기반 구성 파일 관리
- ✅ JPA 도메인 엔티티 및 리포지토리 계층
- ✅ 포괄적 테스트 환경 (단위/통합/리포지토리 테스트)

### 최종 커밋 정보
- **커밋 해시**: d73de38
- **변경 파일**: 44개 파일
- **추가 라인**: 5,044줄 
- **삭제 라인**: 713줄

## 🧪 현재 테스트 가능한 기능

### 1. 단위 테스트 (Unit Tests)
```bash
# 모든 단위 테스트 실행
mvn test -Dtest="*Test"

# 특정 서비스 테스트
mvn test -Dtest="ArticleServiceTest"
mvn test -Dtest="ContentExtractionServiceTest" 
mvn test -Dtest="OpenAiClientTest"
```

**테스트 가능한 컴포넌트**:
- **도메인 엔티티**: Article, Summary 객체 생성/업데이트/관계 관리
- **서비스 계층**: ArticleService의 URL 기반 Upsert 로직
- **컨텐츠 추출**: ContentExtractionService의 웹 스크래핑 로직
- **OpenAI 클라이언트**: Structured Outputs 기반 AI 요약 생성
- **배치 유틸리티**: ExecutionContextUtil의 컨텍스트 관리
- **DTO 변환**: 엔티티 ↔ DTO 매핑 로직

### 2. 통합 테스트 (Integration Tests)
```bash
# H2 데이터베이스 기반 통합 테스트
mvn test -Dtest="*IntegrationTest"

# 특정 통합 테스트
mvn test -Dtest="RssFeedServiceIntegrationTest"
mvn test -Dtest="BatchJobIntegrationTest"
```

**테스트 가능한 시나리오**:
- **RSS 피드 수집**: 실제 RSS URL에서 피드 파싱 및 저장
- **배치 작업 흐름**: S1_FETCH → S2_EXTRACT → S3_SUMMARIZE_AI 전체 파이프라인
- **데이터베이스 연동**: JPA 엔티티 저장/조회/업데이트
- **트랜잭션 처리**: @Transactional 어노테이션 기반 롤백 테스트

### 3. 리포지토리 테스트 (Repository Tests)
```bash
# JPA 리포지토리 테스트
mvn test -Dtest="*RepositoryTest"
```

**테스트 가능한 쿼리**:
- **Article Repository**: 14개 커스텀 쿼리 메서드
- **Summary Repository**: JSONB/배열 타입 데이터 처리
- **통계 쿼리**: 소스별 기사 수, 요약 성공률 등
- **복합 조건 검색**: 날짜 범위, 키워드, 소스별 필터링

### 4. 설정 검증 테스트
```bash
# 설정 파일 로딩 테스트
mvn test -Dtest="ConfigurationPropertiesTest"
```

**검증 가능한 설정**:
- **RSS Sources**: 13개 한국 뉴스 소스 설정
- **Economic Keywords**: 70+ 경제 관련 키워드
- **OpenAI Configuration**: API 키, 모델, 프롬프트 설정
- **Database Configuration**: PostgreSQL/H2 듀얼 지원

## 🐳 개발환경 컨테이너 구성

### 1. 필수 요구사항

#### 시스템 요구사항
```yaml
Java: OpenJDK 8 또는 11
Maven: 3.6+
Docker: 20.10+
Docker Compose: 1.29+
메모리: 최소 4GB RAM
디스크: 최소 5GB 여유 공간
```

#### 환경 변수 설정
```bash
# .env 파일 생성
OPENAI_API_KEY=sk-your-openai-api-key
DATABASE_URL=jdbc:postgresql://localhost:5432/econdigest
DATABASE_USERNAME=econdigest_user
DATABASE_PASSWORD=your-secure-password
SPRING_PROFILES_ACTIVE=dev
```

### 2. Docker Compose 구성

#### PostgreSQL 데이터베이스
```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:14-alpine
    container_name: econdigest-db
    environment:
      POSTGRES_DB: econdigest
      POSTGRES_USER: econdigest_user
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U econdigest_user -d econdigest"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: econdigest-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

### 3. 컨테이너 실행 순서

#### Step 1: 데이터베이스 컨테이너 시작
```bash
# PostgreSQL 컨테이너 실행
docker-compose up -d postgres

# 데이터베이스 연결 확인
docker-compose logs postgres

# 헬스체크 대기
docker-compose ps
```

#### Step 2: 데이터베이스 마이그레이션
```bash
# Flyway 마이그레이션 실행
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/flyway.conf

# 마이그레이션 상태 확인
mvn flyway:info
```

#### Step 3: 애플리케이션 빌드 및 테스트
```bash
# 전체 프로젝트 빌드
mvn clean compile

# 단위 테스트 실행 (H2 사용)
mvn test

# 통합 테스트 실행 (PostgreSQL 연결)
mvn test -Dspring.profiles.active=test-postgres
```

### 4. 개발 서버 실행

#### 로컬 개발 모드
```bash
# Spring Boot 개발 서버 실행
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 또는 JAR 실행
mvn package -DskipTests
java -jar target/econdigest-*.jar --spring.profiles.active=dev
```

#### 배치 작업 실행
```bash
# 전체 다이제스트 생성 배치 실행
java -jar target/econdigest-*.jar \
  --job.name=ECON_DAILY_DIGEST \
  --spring.profiles.active=dev

# 특정 스텝만 실행
java -jar target/econdigest-*.jar \
  --job.name=ECON_DAILY_DIGEST \
  --spring.batch.job.enabled=true \
  --spring.profiles.active=dev
```

### 5. 데이터베이스 연결 확인

#### PostgreSQL 연결 테스트
```bash
# 컨테이너 내에서 psql 실행
docker exec -it econdigest-db psql -U econdigest_user -d econdigest

# 테이블 확인
\dt

# 샘플 데이터 확인
SELECT COUNT(*) FROM articles;
SELECT COUNT(*) FROM summaries;
```

#### 애플리케이션 연결 확인
```bash
# 헬스 체크 엔드포인트 (향후 구현 예정)
curl http://localhost:8080/actuator/health

# 데이터베이스 연결 상태 확인
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.org.hibernate.SQL=DEBUG"
```

## 🔧 설정 파일 구성

### 1. 핵심 설정 파일

#### application.yml
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/econdigest}
    username: ${DATABASE_USERNAME:econdigest_user}
    password: ${DATABASE_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL10Dialect
        format_sql: true
        
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: never
```

#### RSS Sources Configuration (src/main/resources/config/rss-sources.yml)
- **13개 주요 한국 뉴스 소스**
- **70+ 경제 관련 키워드 필터**
- **소스별 우선순위 및 카테고리 설정**

### 2. 프로파일별 설정

#### 개발 환경 (application-dev.yml)
```yaml
logging:
  level:
    com.yourco.econdigest: DEBUG
    org.springframework.batch: DEBUG
    
openai:
  api-key: ${OPENAI_API_KEY}
  model: "gpt-3.5-turbo"
  temperature: 0.3
```

#### 테스트 환경 (application-test.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## 📊 모니터링 및 로깅

### 1. 배치 작업 모니터링
```bash
# 배치 실행 로그 확인
tail -f logs/econdigest.log

# Spring Batch 메타 테이블 조회
SELECT * FROM batch_job_instance ORDER BY job_instance_id DESC LIMIT 10;
SELECT * FROM batch_job_execution WHERE job_instance_id = 1;
```

### 2. 성능 메트릭
- **RSS 피드 수집률**: 소스별 성공/실패 통계
- **본문 추출 성공률**: ContentExtractionService 성능
- **AI 요약 처리량**: OpenAI API 호출 성공률
- **전체 처리 시간**: 배치 작업별 실행 시간

## 🚀 다음 단계 (Week 2 준비사항)

### 1. 운영 환경 구성
- Production 프로파일 설정
- 로그 수집 및 모니터링 시스템
- CI/CD 파이프라인 구축

### 2. 추가 기능 구현
- 웹 UI 대시보드
- REST API 엔드포인트
- 실시간 알림 시스템
- 사용자 관리 기능

### 3. 성능 최적화
- 배치 처리 병렬화
- 데이터베이스 인덱스 최적화
- 캐싱 전략 구현
- API 속도 제한 관리

---

## 📞 문의 및 지원

개발환경 구성 중 문제가 발생하면 다음을 확인하세요:

1. **Java/Maven 버전** 호환성
2. **Docker 컨테이너** 상태 및 로그
3. **환경변수** 설정 (특히 OpenAI API 키)
4. **데이터베이스 연결** 및 마이그레이션 상태
5. **포트 충돌** (5432, 8080 등)

모든 설정이 정상이면 `mvn test` 명령으로 테스트 실행이 가능하며, `mvn spring-boot:run`으로 개발 서버를 시작할 수 있습니다.