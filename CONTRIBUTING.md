# 기여 가이드 (Contributing Guide)

EconoNyang 프로젝트에 기여해주셔서 감사합니다! 🐱

## 🤝 기여 방법

### 1. 이슈 신고
- [GitHub Issues](https://github.com/limcpf/econo-nyang/issues)에서 버그 신고나 기능 요청
- 이슈 템플릿을 활용하여 상세한 정보 제공

### 2. Pull Request 절차
1. **Fork & Clone**
   ```bash
   git clone https://github.com/[your-username]/econo-nyang.git
   cd econo-nyang
   ```

2. **브랜치 생성**
   ```bash
   git checkout -b feature/amazing-feature
   # 또는
   git checkout -b fix/bug-description
   ```

3. **개발 환경 설정**
   ```bash
   cp .env.example .env
   # .env 파일에 필요한 환경변수 설정
   
   docker-compose up -d
   ./mvnw clean compile
   ```

4. **코드 작성 및 테스트**
   ```bash
   # TDD 기반 개발
   ./mvnw test
   ```

5. **커밋 & Push**
   ```bash
   git add .
   git commit -m "feat: Add amazing feature"
   git push origin feature/amazing-feature
   ```

6. **Pull Request 생성**
   - 제목: `feat/fix/docs: 간단한 설명`
   - 템플릿에 따라 상세 정보 작성

## 📋 개발 가이드라인

### 코딩 스타일
- **Java**: Google Java Style Guide 준수
- **네이밍**: 명확하고 의미 있는 이름 사용
- **주석**: 코드의 "왜"를 설명 (What이 아닌 Why)

### 테스트 요구사항
- **TDD**: Red → Green → Refactor 사이클 준수
- **커버리지**: 라인 커버리지 80% 이상
- **테스트 명명**: `given_when_then` 또는 `should_` 패턴

### 커밋 메시지 컨벤션
```
type: 제목 (영문/한글 모두 가능)

상세 설명 (선택사항)

🤖 Generated with [Claude Code](https://claude.ai/code)
Co-Authored-By: Claude <noreply@anthropic.com>
```

**Type 종류:**
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 변경
- `style`: 코드 포맷팅, 세미콜론 누락 등
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가
- `chore`: 빌드 과정 또는 보조 기능 수정

## 🏗️ 프로젝트 구조

```
src/
├── main/java/com/yourco/econyang/
│   ├── batch/           # Spring Batch 설정 및 단계
│   ├── config/          # 설정 클래스들
│   ├── domain/          # JPA 엔티티
│   ├── repository/      # 데이터 접근 계층
│   ├── service/         # 비즈니스 로직
│   ├── openai/          # OpenAI API 연동
│   └── util/           # 유틸리티 클래스
└── test/java/          # 테스트 코드
```

## 🧪 테스트 실행

```bash
# 전체 테스트
./mvnw test

# 특정 테스트 클래스
./mvnw test -Dtest=ArticleServiceTest

# 통합 테스트
./mvnw test -Dtest="*IntegrationTest"
```

## 📚 참고 문서

- [CLAUDE.md](./CLAUDE.md): 개발 가이드라인 상세
- [PRD](./docs/00-PRD-20250817.md): 제품 요구사항
- [Architecture](./docs/01-Detail-Structure-20250817.md): 시스템 설계

## ❓ 질문이 있나요?

- **GitHub Discussions**: 일반적인 질문과 토론
- **Issues**: 버그 신고 및 기능 요청
- **Email**: limcpf@gmail.com

---

함께 만들어가는 EconoNyang! 🐱