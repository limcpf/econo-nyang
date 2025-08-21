# EconoNyang 실제 모드 구현 방법

**문서명**: EconoNyang 더미 모드에서 실제 RSS 수집 및 Discord 발송 모드로 변경  
**작성일**: 2025-08-21  
**목적**: 현재 더미 처리되는 배치를 실제 데이터 처리 및 발송이 가능하도록 변경

---

## 1. 현재 상황 분석

### ✅ 해결된 부분
- Spring Boot 애플리케이션 정상 시작
- Spring Batch 메타데이터 테이블 생성 및 Job 실행 
- PostgreSQL 데이터베이스 연결 성공
- 5단계 배치 파이프라인 구조 완성

### ❌ 더미 처리 중인 부분
- S1_FETCH: 실제 RSS 수집 대신 mockFetchedCount = 10
- S2_EXTRACT: 실제 본문 추출 대신 카운트만 전달
- S3_SUMMARIZE_AI: OpenAI API 호출 없이 더미 완료
- S4_RANK_COMPOSE: 실제 중요도 산정 없이 Math.min() 처리
- S5_DISPATCH: Discord 발송 없이 더미 완료 메시지

---

## 2. 핵심 문제: ExecutionContext 직렬화

### 🔍 문제 원인
```java
// 현재 문제가 되는 코드 (BatchConfiguration.java 구버전)
ExecutionContextUtil.putToJobContext(
    chunkContext.getStepContext().getStepExecution(),
    "fetchedArticles", 
    filteredArticles  // ← List<ArticleDto> 직렬화 실패
);
```

**오류 메시지**: 
```
Java 8 date/time type `java.time.LocalDateTime` not supported by default
Jackson 직렬화 오류 - ArticleDto의 LocalDateTime 필드
```

### 💡 해결 방안

#### Option 1: 데이터베이스 저장 방식 (추천)
```java
// Step 1에서 기사들을 DB에 저장
List<Article> savedArticles = articleService.saveArticles(filteredArticles);

// ExecutionContext에는 ID만 저장
List<Long> articleIds = savedArticles.stream()
    .map(Article::getId)
    .collect(Collectors.toList());

ExecutionContextUtil.putToJobContext(execution, "articleIds", articleIds);
```

#### Option 2: DTO 직렬화 가능하게 수정
```java
// ArticleDto에 Jackson 어노테이션 추가
public class ArticleDto implements Serializable {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") 
    private LocalDateTime fetchedAt;
    // ... 기타 필드
}
```

#### Option 3: 간단한 데이터만 전달
```java
// 복잡한 객체 대신 기본 정보만 전달
Map<String, Object> stepData = new HashMap<>();
stepData.put("articleCount", articles.size());
stepData.put("processedUrls", urls);
stepData.put("batchId", batchId);
```

---

## 3. 단계별 실제 구현 방법

### 🔄 S1_FETCH: RSS 피드 수집
```java
@Bean
public Step step1Fetch() {
    return stepBuilderFactory.get("S1_FETCH")
        .tasklet((contribution, chunkContext) -> {
            // 1. RSS 소스에서 실제 기사 수집
            List<ArticleDto> articles = rssFeedService.fetchAllArticles(
                rssSourcesConfig.getSources(), maxArticles);
            
            // 2. 키워드 필터링
            List<ArticleDto> filtered = rssFeedService.applyFilters(
                articles, rssSourcesConfig.getFilters());
            
            // 3. 데이터베이스에 저장 (중복 방지)
            List<Long> articleIds = articleService.saveNewArticles(filtered);
            
            // 4. ExecutionContext에 ID만 저장
            ExecutionContextUtil.putToJobContext(execution, "articleIds", articleIds);
            ExecutionContextUtil.putToJobContext(execution, "fetchedCount", articleIds.size());
            
            return RepeatStatus.FINISHED;
        }).build();
}
```

### 📄 S2_EXTRACT: 본문 추출
```java
@Bean  
public Step step2Extract() {
    return stepBuilderFactory.get("S2_EXTRACT")
        .tasklet((contribution, chunkContext) -> {
            // 1. 이전 단계에서 저장된 기사 ID 조회
            List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                execution, "articleIds", List.class);
            
            // 2. DB에서 기사 조회
            List<Article> articles = articleService.findByIds(articleIds);
            
            // 3. 본문 추출 (병렬 처리)
            articles.parallelStream().forEach(article -> {
                if (article.getContent() == null) {
                    try {
                        String content = contentExtractionService.extractContent(article.getUrl());
                        article.setContent(content);
                        article.setExtractedAt(LocalDateTime.now());
                        articleService.save(article);
                    } catch (Exception e) {
                        article.setExtractError(e.getMessage());
                        articleService.save(article);
                    }
                }
            });
            
            return RepeatStatus.FINISHED;
        }).build();
}
```

### 🤖 S3_SUMMARIZE_AI: AI 요약 생성  
```java
@Bean
public Step step3SummarizeAi() {
    return stepBuilderFactory.get("S3_SUMMARIZE_AI")
        .tasklet((contribution, chunkContext) -> {
            // 1. 본문이 추출된 기사들 조회
            List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                execution, "articleIds", List.class);
            List<Article> articles = articleService.findExtractedByIds(articleIds);
            
            // 2. OpenAI API 호출하여 요약 생성
            String useLLM = jobParameters.get("useLLM").toString();
            if (!"false".equals(useLLM)) {
                for (Article article : articles) {
                    try {
                        EconomicSummaryResponse summary = openAiClient.generateSummary(
                            article.getTitle(), article.getContent());
                        
                        // Summary 엔티티 생성 및 저장
                        Summary summaryEntity = new Summary(article, "gpt-4o-mini",
                            summary.getSummary(), summary.getWhyItMatters());
                        summaryEntity.setBullets(summary.getBullets().toArray(new String[0]));
                        summaryEntity.setScore(BigDecimal.valueOf(summary.getImportanceScore()));
                        
                        summaryService.save(summaryEntity);
                        
                    } catch (Exception e) {
                        System.err.println("AI 요약 실패: " + article.getTitle() + " - " + e.getMessage());
                    }
                }
            }
            
            return RepeatStatus.FINISHED;
        }).build();
}
```

### 📊 S4_RANK_COMPOSE: 중요도 산정 및 선별
```java  
@Bean
public Step step4RankCompose() {
    return stepBuilderFactory.get("S4_RANK_COMPOSE") 
        .tasklet((contribution, chunkContext) -> {
            // 1. 요약된 기사들 조회
            List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                execution, "articleIds", List.class);
            List<Summary> summaries = summaryService.findByArticleIds(articleIds);
            
            // 2. 중요도 순 정렬
            summaries.sort((s1, s2) -> s2.getScore().compareTo(s1.getScore()));
            
            // 3. 상위 N개 선별
            int maxArticles = Integer.parseInt(jobParameters.get("maxArticles").toString());
            List<Summary> topSummaries = summaries.stream()
                .limit(maxArticles)
                .collect(Collectors.toList());
            
            // 4. DailyDigest 생성
            String targetDate = jobParameters.get("targetDate").toString();
            DailyDigest digest = new DailyDigest();
            digest.setTargetDate(LocalDate.parse(targetDate));
            digest.setStatus(DailyDigest.Status.COMPOSED);
            digest.setArticleCount(topSummaries.size());
            
            // 5. 마크다운 다이제스트 생성
            String markdownContent = digestTemplateService.generateMarkdown(
                topSummaries, digestTemplateConfig.getTemplates().get("default"));
            digest.setContent(markdownContent);
            
            DailyDigest savedDigest = dailyDigestService.save(digest);
            
            // 6. 다음 단계로 다이제스트 ID 전달
            ExecutionContextUtil.putToJobContext(execution, 
                "digestId", savedDigest.getId());
            
            return RepeatStatus.FINISHED;
        }).build();
}
```

### 💬 S5_DISPATCH: Discord 발송
```java
@Bean
public Step step5Dispatch() {
    return stepBuilderFactory.get("S5_DISPATCH")
        .tasklet((contribution, chunkContext) -> {
            // 1. 생성된 다이제스트 조회  
            Long digestId = ExecutionContextUtil.getFromJobContext(
                execution, "digestId", Long.class);
            DailyDigest digest = dailyDigestService.findById(digestId);
            
            // 2. Discord Webhook 발송
            String dryRun = jobParameters.get("dryRun").toString();
            if (!"true".equals(dryRun)) {
                try {
                    DiscordWebhookResponse response = discordService.sendDigest(
                        digest.getContent(), "경제냥이 다이제스트 🐱");
                    
                    // 3. 발송 로그 기록
                    DispatchLog log = new DispatchLog(digest, "discord", 
                        DispatchLog.Status.SUCCESS);
                    log.setResponseSnippet(response.toString());
                    dispatchLogService.save(log);
                    
                    System.out.println("Discord 발송 성공: " + response.getId());
                    
                } catch (Exception e) {
                    DispatchLog log = new DispatchLog(digest, "discord", 
                        DispatchLog.Status.FAILED);
                    log.setErrorMessage(e.getMessage());
                    dispatchLogService.save(log);
                    
                    System.err.println("Discord 발송 실패: " + e.getMessage());
                }
            } else {
                System.out.println("DRY RUN: Discord 발송 건너뜀");
            }
            
            return RepeatStatus.FINISHED;
        }).build();
}
```

---

## 4. Discord Webhook 설정

### 🔗 Discord 서버 설정
1. Discord 서버에서 "서버 설정" → "연동" → "웹후크"
2. "웹후크 만들기" 클릭
3. 이름: "EconoNyang Bot" 🐱
4. 채널 선택 (예: #경제뉴스)
5. 웹후크 URL 복사

### ⚙️ 환경변수 설정
```bash
# .env 파일에 추가
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN
```

### 💻 Discord 클라이언트 구현 필요
```java
@Service
public class DiscordService {
    
    @Value("${app.discord.webhook.url:}")
    private String webhookUrl;
    
    public DiscordWebhookResponse sendDigest(String content, String title) {
        // Discord Embed 메시지 생성 및 발송
        // 2000자 제한 대응 (메시지 분할)
        // 재시도 로직 (Rate Limit 대응)
    }
}
```

---

## 5. 실행 방법

### 🧪 테스트 실행 (DRY RUN)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--batch.auto-run=true"
```

### 🚀 실제 실행 (LIVE MODE) 
```bash  
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--batch.auto-run=true --dryRun=false --useLLM=true"
```

### 🎛️ 매개변수 설정
```bash
java -jar target/econyang-*.jar \
  --job.name=ECON_DAILY_DIGEST \
  --targetDate=2025-08-21 \
  --maxArticles=10 \
  --dryRun=false \
  --useLLM=true \
  --forceRefresh=false
```

---

## 6. 예상 처리 시간

| 단계 | 더미 모드 | 실제 모드 | 비고 |
|------|-----------|-----------|------|
| S1_FETCH | 0.01초 | 5-15초 | RSS 수집, 네트워크 지연 |
| S2_EXTRACT | 0.01초 | 30-60초 | 웹 스크래핑, 병렬 처리 |
| S3_SUMMARIZE_AI | 0.01초 | 60-120초 | OpenAI API 호출 |
| S4_RANK_COMPOSE | 0.01초 | 1-5초 | 중요도 계산 및 정렬 |
| S5_DISPATCH | 0.01초 | 1-3초 | Discord API 호출 |
| **전체** | **0.05초** | **2-4분** | 10-15개 기사 기준 |

---

## 7. 구현 우선순위

### 🏆 1단계 (필수)
1. **ExecutionContext 직렬화 해결** - 데이터베이스 저장 방식 적용
2. **S1_FETCH 실제 구현** - RSS 수집 및 필터링
3. **Discord 서비스 구현** - Webhook 발송 기능

### 🥈 2단계 (중요) 
4. **S2_EXTRACT 실제 구현** - 본문 추출 서비스 연동
5. **S5_DISPATCH 실제 구현** - Discord 발송 및 로깅

### 🥉 3단계 (선택)
6. **S3_SUMMARIZE_AI 실제 구현** - OpenAI API 연동
7. **S4_RANK_COMPOSE 실제 구현** - 중요도 알고리즘

---

## 8. 리스크 및 대응 방안

### ⚠️ 주요 리스크
1. **OpenAI API 비용** - Batch API 사용으로 50% 절감
2. **RSS 사이트 차단** - User-Agent 순환, 지연 시간 추가
3. **Discord Rate Limit** - 재시도 로직, 지수 백오프
4. **메모리 부족** - 배치 처리, DB 저장 우선

### 🛡️ 안전 장치
- **DRY RUN 모드**: 실제 발송 없이 테스트
- **최대 처리 한도**: maxArticles로 처리량 제한  
- **타임아웃 설정**: 각 단계별 제한 시간
- **에러 복구**: 재시도 로직 및 폴백 처리

---

**다음 단계**: 위 방법 중 1단계부터 순서대로 구현하여 점진적으로 실제 모드로 전환