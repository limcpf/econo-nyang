# EconoNyang 🐱

> **매일 너에게 경제뉴스를 소개해주는 귀여운 친구**

EconoNyang은 Spring Batch와 OpenAI API를 활용하여 한국 경제뉴스를 자동으로 수집, 요약, 분석하여 매일 아침 Discord로 전달하는 똑똑한 경제뉴스 봇입니다.

## ✨ 주요 기능

- 🔄 **자동 뉴스 수집**: 13개 주요 한국 언론사 RSS 피드 모니터링
- 🤖 **AI 요약 & 분석**: OpenAI GPT 모델로 경제뉴스 요약 및 시장 영향 분석  
- 📊 **중요도 산정**: 출처 신뢰도, 키워드 매칭, 교차 검증으로 중요 뉴스 선별
- 💬 **Discord 알림**: 매일 오전 7:30 자동 발송
- 🏗️ **Spring Batch**: 안정적이고 확장 가능한 배치 처리
- 🐘 **PostgreSQL**: 견고한 데이터 저장 및 인덱싱

## 🎯 대상 사용자

- 경제뉴스를 매일 체크하고 싶지만 시간이 부족한 직장인
- 경제 초보자도 이해하기 쉬운 요약과 해설이 필요한 사람
- Discord를 통해 편리하게 뉴스를 받고 싶은 팀/개인

## 🏗️ 아키텍처

```
RSS 수집 → 본문 추출 → AI 요약 → 중요도 산정 → Discord 발송
   ↓           ↓         ↓         ↓           ↓
 S1_FETCH → S2_EXTRACT → S3_SUMMARIZE → S4_RANK → S5_DISPATCH
```

### 기술 스택
- **Backend**: Java 8, Spring Boot 2.7.x, Spring Batch 4.3.x
- **Database**: PostgreSQL 14+, Flyway Migration
- **AI**: OpenAI API (GPT-4o, GPT-4o-mini) with Structured Outputs
- **Messaging**: Discord Webhook
- **Containerization**: Docker, Docker Compose
- **CI/CD**: Jenkins Pipeline

## 🚀 빠른 시작

### 완전한 설치 및 운영 가이드
👉 **[운영 가이드](./docs/06-운영-가이드.md)**에서 상세한 설치, 설정, 운영 방법을 확인하세요!

### 간단 실행 (요약)

```bash
# 1. 프로젝트 클론 및 환경 설정
git clone https://github.com/limcpf/econo-nyang.git
cd econo-nyang
cp .env.example .env
# .env 파일에서 OPENAI_API_KEY, DISCORD_WEBHOOK_URL 설정

# 2. 데이터베이스 실행 및 마이그레이션  
docker-compose up -d postgres
sleep 30
./mvnw flyway:migrate

# 3. 애플리케이션 빌드 및 실행
./mvnw clean package -DskipTests
java -jar target/econyang-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --job.name=ECON_DAILY_DIGEST \
  --maxArticles=10 --useLLM=true
```

### 개발 모드 실행

```bash
# 개발 환경에서 테스트
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--dryRun=true"
```

## 📋 배치 작업 단계

| Step | 설명 | 입력 | 출력 |
|------|------|------|------|
| **S1_FETCH** | RSS 피드 수집 | RSS URLs | Article 목록 |
| **S2_EXTRACT** | 본문 추출 | Article URLs | 전문 텍스트 |
| **S3_SUMMARIZE_AI** | AI 요약 생성 | 기사 본문 | 구조화된 요약 |
| **S4_RANK_COMPOSE** | 중요도 산정 | 요약된 기사들 | 선별된 다이제스트 |
| **S5_DISPATCH** | Discord 발송 | 다이제스트 | 메시지 전송 |

## 🔧 설정

### RSS 소스 설정
```yaml
# src/main/resources/config/rss-sources.yml
sources:
  - name: "한국경제"
    url: "https://www.hankyung.com/feed/economy"
    weight: 1.2
    enabled: true
```

### AI 모델 설정
```yaml
# application.yml
app:
  openai:
    modelSmall: gpt-4o-mini     # 1차 요약용
    modelMain: gpt-4o           # 최종 다듬기용
    enableBatch: true           # Batch API 사용 (50% 비용 절감)
```

## 📊 모니터링

### 배치 실행 현황
```sql
-- 최근 배치 실행 이력
SELECT * FROM batch_job_execution 
ORDER BY create_time DESC LIMIT 10;

-- 일일 처리 통계
SELECT 
  DATE(created_at) as date,
  COUNT(*) as articles_processed,
  AVG(importance_score) as avg_importance
FROM summaries 
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

### 주요 메트릭
- **처리 성능**: 500건/5분 목표
- **추출 성공률**: 80% 이상
- **AI 요약 성공률**: 95% 이상
- **Discord 발송 성공률**: 99% 이상

## 🤝 기여하기

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### 개발 가이드라인
- TDD 기반 개발 (Red → Green → Refactor)
- 테스트 커버리지 80% 이상 유지
- 코드 리뷰 필수
- [CLAUDE.md](./CLAUDE.md) 가이드라인 준수

## 📚 문서

### 📖 사용자 가이드
- [🚀 **운영 가이드**](./docs/06-운영-가이드.md) - **프로덕션 환경 구동 및 운영 매뉴얼**
- [🛠️ 개발환경 구성 가이드](./docs/03-Week1-doc.md)

### 📋 개발 문서  
- [📋 PRD (Product Requirements Document)](./docs/00-PRD-20250817.md)
- [🏗️ 상세 설계서](./docs/01-Detail-Structure-20250817.md) 
- [✅ Week1 개발 현황](./docs/02-Week1-Tasks-20250817.md)
- [✅ Week2 개발 현황](./docs/05-Week2-Tasks.md)
- [📖 개발 가이드라인](./CLAUDE.md)

## 🔒 보안 및 프라이버시

- OpenAI API Key는 환경변수로만 관리
- 기사 전문은 저장하지 않고 요약만 보관
- 개인정보 수집하지 않음
- HTTPS 통신 강제

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🐛 이슈 신고

버그나 기능 요청이 있으시면 [GitHub Issues](https://github.com/limcpf/econo-nyang/issues)에 등록해주세요.

## 📞 문의

- **Developer**: [@limcpf](https://github.com/limcpf)
- **Email**: limcpf@gmail.com

---

**EconoNyang** 🐱 - 매일 아침 경제뉴스와 함께 시작하세요!