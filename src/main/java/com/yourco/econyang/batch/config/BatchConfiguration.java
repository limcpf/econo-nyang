package com.yourco.econyang.batch.config;

import com.yourco.econyang.batch.util.ExecutionContextUtil;
import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.service.ArticleService;
import com.yourco.econyang.service.ContentExtractionService;
import com.yourco.econyang.service.DiscordService;
import com.yourco.econyang.service.DigestTemplateService;
import com.yourco.econyang.service.ImportanceRankingService;
import com.yourco.econyang.service.SummaryService;
import com.yourco.econyang.repository.SummaryRepository;
import com.yourco.econyang.repository.DailyDigestRepository;
import com.yourco.econyang.domain.DailyDigest;
import com.yourco.econyang.domain.Summary;
import com.yourco.econyang.service.RssFeedService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.yourco.econyang.domain.Article;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch Job 설정
 * ECON_DAILY_DIGEST Job과 관련 Step들을 정의합니다.
 */
@Configuration
public class BatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private EconDigestJobParametersValidator jobParametersValidator;

    @Autowired
    private RssSourcesConfig rssSourcesConfig;

    @Autowired
    private RssFeedService rssFeedService;

    @Autowired
    private ContentExtractionService contentExtractionService;

    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DiscordService discordService;
    
    @Autowired
    private DigestTemplateService digestTemplateService;
    
    @Autowired
    private ImportanceRankingService importanceRankingService;
    
    @Autowired
    private SummaryRepository summaryRepository;
    
    @Autowired
    private DailyDigestRepository dailyDigestRepository;
    
    @Autowired
    private SummaryService summaryService;

    /**
     * ECON_DAILY_DIGEST Job 정의
     * 경제뉴스 다이제스트 생성을 위한 메인 Job
     */
    @Bean
    public Job econDailyDigestJob() {
        return jobBuilderFactory.get("ECON_DAILY_DIGEST")
                .incrementer(new RunIdIncrementer())
                .validator(jobParametersValidator)
                .start(step1Fetch())
                .next(step2Extract())
                .next(step3SummarizeAi())
                .next(step4RankCompose())
                .next(step5Dispatch())
                .build();
    }

    /**
     * S1_FETCH Step - RSS 피드 수집 (실제/더미 모드 지원)
     */
    @Bean
    public Step step1Fetch() {
        return stepBuilderFactory.get("S1_FETCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S1_FETCH: RSS 피드 수집 시작 ===");
                    
                    long startTime = System.currentTimeMillis();
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_START_TIME,
                            startTime
                    );
                    
                    // JobParameter에서 실제 수집 여부 확인
                    String useRealRss = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("useRealRss") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("useRealRss").toString() : 
                            "false";
                    
                    List<Long> articleIds = new ArrayList<>();
                    
                    if ("true".equals(useRealRss)) {
                        try {
                            // 실제 RSS 수집
                            System.out.println("실제 RSS 수집 모드 시작");
                            
                            // Job Parameters에서 최대 기사 수 가져오기
                            String maxArticlesParam = chunkContext.getStepContext()
                                    .getJobParameters()
                                    .get("maxArticles") != null ? 
                                    chunkContext.getStepContext().getJobParameters().get("maxArticles").toString() : 
                                    "10";
                            int maxArticles = Integer.parseInt(maxArticlesParam);
                            
                            // RSS 소스에서 기사 수집
                            List<ArticleDto> articles = rssFeedService.fetchAllArticles(
                                    rssSourcesConfig.getSources(), maxArticles);
                            
                            // 키워드 필터링 적용
                            List<ArticleDto> filteredArticles = rssFeedService.applyFilters(
                                    articles, rssSourcesConfig.getFilters());
                            
                            // 데이터베이스에 저장
                            articleIds = articleService.saveNewArticles(filteredArticles);
                            
                            System.out.println("실제 RSS 수집 완료: " + articles.size() + "개 수집 → " + 
                                             filteredArticles.size() + "개 필터링 → " + 
                                             articleIds.size() + "개 저장");
                            
                        } catch (Exception e) {
                            System.err.println("RSS 수집 실패, 더미 데이터로 폴백: " + e.getMessage());
                            e.printStackTrace();
                            articleIds = createDummyArticles();
                        }
                    } else {
                        // 더미 데이터 모드
                        System.out.println("더미 데이터 모드");
                        articleIds = createDummyArticles();
                    }
                    
                    // ExecutionContext에 기사 ID 목록 저장 (직렬화 안전)
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "articleIds",
                            articleIds
                    );
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                            articleIds.size()
                    );
                    
                    System.out.println("S1_FETCH 완료: " + articleIds.size() + "개 기사 수집 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    /**
     * 더미 기사 데이터 생성 및 저장
     */
    private List<Long> createDummyArticles() {
        List<ArticleDto> dummyArticles = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            ArticleDto article = new ArticleDto();
            article.setSource("한국경제");
            article.setUrl("https://example.com/article/" + i);
            article.setTitle("더미 경제뉴스 제목 " + i);
            article.setAuthor("기자" + i);
            article.setDescription("더미 경제뉴스 요약 내용 " + i);
            dummyArticles.add(article);
        }
        
        return articleService.saveNewArticles(dummyArticles);
    }

    /**
     * S2_EXTRACT Step - 본문 추출 및 정제
     */
    @Bean
    public Step step2Extract() {
        return stepBuilderFactory.get("S2_EXTRACT")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S2_EXTRACT: 본문 추출 및 정제 시작 ===");
                    
                    // 이전 Step에서 저장된 기사 ID 목록 조회
                    @SuppressWarnings("unchecked")
                    List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "articleIds",
                            List.class
                    );
                    
                    if (articleIds == null || articleIds.isEmpty()) {
                        System.out.println("S2_EXTRACT: 처리할 기사가 없습니다.");
                        return RepeatStatus.FINISHED;
                    }
                    
                    // DB에서 기사들 조회
                    List<Article> articles = articleService.findByIds(articleIds);
                    System.out.println("DB에서 " + articles.size() + "개 기사 로드");
                    
                    // Job Parameter에서 실제 추출 여부 확인
                    String useRealExtraction = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("useRealExtraction") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("useRealExtraction").toString() : 
                            "false";
                    
                    int extractedCount = 0;
                    
                    if ("true".equals(useRealExtraction)) {
                        // 실제 본문 추출
                        System.out.println("실제 본문 추출 모드 시작");
                        
                        try {
                            // Article → ArticleDto 변환
                            List<ArticleDto> articleDtos = articles.stream()
                                    .filter(article -> article.getContent() == null || article.getContent().trim().isEmpty())
                                    .map(this::convertToDto)
                                    .collect(java.util.stream.Collectors.toList());
                            
                            if (!articleDtos.isEmpty()) {
                                System.out.println("본문 추출 대상: " + articleDtos.size() + "개 기사");
                                
                                // 병렬 본문 추출 실행
                                List<ArticleDto> extractedDtos = contentExtractionService.extractContents(articleDtos);
                                
                                // 결과를 DB에 저장
                                for (ArticleDto dto : extractedDtos) {
                                    articleService.findByUrl(dto.getUrl()).ifPresent(article -> {
                                        if (dto.isExtractSuccess()) {
                                            article.setContent(dto.getContent());
                                            article.setExtractedAt(dto.getExtractedAt());
                                            article.setExtractError(null);
                                        } else {
                                            article.setExtractError(dto.getExtractError());
                                            article.setExtractedAt(dto.getExtractedAt());
                                        }
                                        articleService.save(article);
                                    });
                                    
                                    if (dto.isExtractSuccess()) {
                                        extractedCount++;
                                    }
                                }
                            } else {
                                System.out.println("모든 기사가 이미 본문을 가지고 있습니다.");
                                extractedCount = articles.size();
                            }
                            
                        } catch (Exception e) {
                            System.err.println("본문 추출 실패, 더미 모드로 폴백: " + e.getMessage());
                            e.printStackTrace();
                            extractedCount = processDummyExtraction(articles);
                        }
                    } else {
                        // 더미 본문 설정
                        System.out.println("더미 본문 추출 모드");
                        extractedCount = processDummyExtraction(articles);
                    }
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            extractedCount
                    );
                    
                    System.out.println("S2_EXTRACT 완료: " + extractedCount + "개 기사 본문 추출 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    /**
     * Article → ArticleDto 변환
     */
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setSource(article.getSource());
        dto.setUrl(article.getUrl());
        dto.setTitle(article.getTitle());
        dto.setDescription(article.getRawExcerpt());
        dto.setAuthor(article.getAuthor());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setContent(article.getContent());
        dto.setExtractedAt(article.getExtractedAt());
        return dto;
    }
    
    /**
     * 더미 본문 추출 처리
     */
    private int processDummyExtraction(List<Article> articles) {
        int extractedCount = 0;
        for (Article article : articles) {
            if (article.getContent() == null || article.getContent().trim().isEmpty()) {
                article.setContent("더미 본문 내용: " + article.getTitle() + "에 대한 상세한 기사 내용입니다. 경제 동향과 관련된 중요한 정보가 포함되어 있습니다.");
                article.setExtractedAt(java.time.LocalDateTime.now());
                article.setExtractError(null);
                articleService.save(article);
                extractedCount++;
            }
        }
        return extractedCount;
    }
    
    /**
     * 더미 다이제스트 처리
     */
    private int processDummyDigest(LocalDate digestDate, String templateName) {
        // 더미로 3-5개 기사 선별한 것으로 가정
        return 4;
    }
    
    /**
     * 더미 다이제스트 본문 생성
     */
    private String generateDummyDigestBody(int articleCount) {
        StringBuilder body = new StringBuilder();
        body.append("# 📈 경제뉴스 다이제스트 - ").append(LocalDate.now()).append("\n\n");
        body.append("총 ").append(articleCount).append("개의 주요 경제 뉴스를 선별했습니다.\n\n");
        body.append("---\n\n");
        
        for (int i = 1; i <= articleCount; i++) {
            body.append("## ").append(i).append("🌟 더미 경제뉴스 제목 ").append(i).append("\n\n");
            body.append("📍 **출처**: 더미소스 | ⏰ **시간**: 09:00\n\n");
            body.append("📝 **요약**: 이것은 더미 요약입니다. 경제 동향에 대한 중요한 내용이 포함되어 있습니다.\n\n");
            body.append("🔍 **AI 분석**: 더미 AI 분석 내용입니다.\n\n");
            body.append("📈 **시장 영향**: 보통 | 👥 **투자자 관심**: 높음\n\n");
            body.append("🏷️ **키워드**: 경제, 뉴스, 더미\n\n");
            body.append("📖 [원문 보기](https://example.com/dummy-").append(i).append(")\n\n");
            body.append("---\n\n");
        }
        
        body.append("📊 **오늘의 통계**\n");
        body.append("- 📰 수집된 뉴스: ").append(articleCount * 2).append("개\n");
        body.append("- ✅ 분석 완료: ").append(articleCount).append("개\n");
        body.append("- ⭐ 평균 중요도: 7.5/10\n\n");
        body.append("🤖 **더미 모드로 생성됨** | ").append(java.time.LocalDateTime.now());
        
        return body.toString();
    }
    
    /**
     * 더미 AI 요약 처리
     */
    private int processDummyAiSummary(int extractedCount) {
        // 더미로 추출된 기사 수와 동일하게 처리
        return Math.min(extractedCount, 5); // 최대 5개
    }

    /**
     * S3_SUMMARIZE_AI Step - AI 요약 생성
     */
    @Bean
    public Step step3SummarizeAi() {
        return stepBuilderFactory.get("S3_SUMMARIZE_AI")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S3_SUMMARIZE_AI: AI 요약 생성 시작 ===");
                    
                    Integer extractedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    // Job Parameter에서 AI 사용 여부 확인
                    String useLLM = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("useLLM") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("useLLM").toString() : 
                            "false";
                    
                    @SuppressWarnings("unchecked")
                    List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "articleIds",
                            List.class
                    );
                    
                    int summarizedCount = 0;
                    
                    if ("true".equals(useLLM) && summaryService.isAiSummaryAvailable()) {
                        // 실제 AI 요약 생성
                        System.out.println("실제 AI 요약 생성 모드");
                        
                        try {
                            // DB에서 본문이 있는 기사들 조회
                            List<Article> articles = articleService.findByIds(articleIds != null ? articleIds : new ArrayList<>());
                            
                            // 본문이 있는 기사만 필터링
                            List<Article> articlesWithContent = articles.stream()
                                    .filter(article -> article.getContent() != null && 
                                           !article.getContent().trim().isEmpty())
                                    .collect(java.util.stream.Collectors.toList());
                            
                            if (!articlesWithContent.isEmpty()) {
                                System.out.println("AI 요약 생성 대상: " + articlesWithContent.size() + "개 기사");
                                
                                // 배치 AI 요약 생성
                                List<Summary> summaries = summaryService.generateSummaries(articlesWithContent);
                                summarizedCount = summaries.size();
                                
                                // 성공한 요약 개수 계산
                                long successCount = summaries.stream()
                                        .mapToLong(summary -> summary.getScore() != null && 
                                            summary.getScore().compareTo(BigDecimal.valueOf(3)) > 0 ? 1 : 0)
                                        .sum();
                                
                                System.out.println("AI 요약 생성 완료: " + successCount + "/" + summarizedCount + " 성공");
                                
                                // API 사용량 통계 출력
                                summaryService.printApiUsageStats();
                            } else {
                                System.out.println("본문이 있는 기사가 없어서 AI 요약을 건너뜁니다.");
                                summarizedCount = 0;
                            }
                            
                        } catch (Exception e) {
                            System.err.println("AI 요약 생성 실패, 더미 모드로 폴백: " + e.getMessage());
                            e.printStackTrace();
                            summarizedCount = processDummyAiSummary(extractedCount != null ? extractedCount : 0);
                        }
                        
                    } else {
                        // 더미 AI 요약 모드
                        System.out.println("더미 AI 요약 모드 (useLLM=false 또는 API 미사용)");
                        summarizedCount = processDummyAiSummary(extractedCount != null ? extractedCount : 0);
                    }
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            summarizedCount
                    );
                    
                    System.out.println("S3_SUMMARIZE_AI 완료: " + summarizedCount + "개 기사 요약 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S4_RANK_COMPOSE Step - 중요도 산정 및 다이제스트 조립
     */
    @Bean
    public Step step4RankCompose() {
        return stepBuilderFactory.get("S4_RANK_COMPOSE")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S4_RANK_COMPOSE: 중요도 산정 및 다이제스트 조립 시작 ===");
                    
                    Integer summarizedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    // Job Parameter에서 다이제스트 생성 모드 확인
                    String useLLM = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("useLLM") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("useLLM").toString() : 
                            "false";
                    
                    String templateName = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("templateName") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("templateName").toString() : 
                            "default";
                    
                    LocalDate digestDate = LocalDate.now();
                    int rankedCount = 0;
                    String digestTitle = "";
                    String digestBody = "";
                    
                    if ("true".equals(useLLM)) {
                        // 실제 Summary 데이터로 다이제스트 생성
                        System.out.println("실제 다이제스트 생성 모드 (Summary 기반)");
                        
                        try {
                            // 최근 생성된 모든 Summary 조회 (최소 점수 1.0 이상)
                            LocalDateTime startTime = digestDate.atStartOfDay();
                            LocalDateTime endTime = startTime.plusDays(1);
                            
                            List<Summary> allSummaries = summaryRepository.findForDigest(
                                    BigDecimal.valueOf(1.0), startTime, endTime);
                            
                            System.out.println("고급 랭킹 알고리즘 적용: " + allSummaries.size() + "개 Summary 대상");
                            
                            // 고급 중요도 산정 알고리즘 적용
                            List<Summary> summaries = importanceRankingService.calculateImportanceRanking(allSummaries);
                            
                            rankedCount = summaries.size();
                            
                            if (rankedCount > 0) {
                                // DigestTemplateService로 다이제스트 생성
                                digestTitle = digestTemplateService.getTemplateTitle(templateName);
                                digestBody = digestTemplateService.generateDigest(summaries, templateName, "markdown");
                                
                                System.out.println("다이제스트 생성 완료: " + rankedCount + "개 Summary 기반");
                            } else {
                                // Summary가 없으면 빈 다이제스트 생성
                                digestTitle = "경제뉴스 다이제스트 - " + digestDate;
                                digestBody = digestTemplateService.generateDigest(new ArrayList<>(), templateName, "markdown");
                                
                                System.out.println("Summary가 없어서 빈 다이제스트 생성");
                            }
                            
                        } catch (Exception e) {
                            System.err.println("실제 다이제스트 생성 실패, 더미 모드로 폴백: " + e.getMessage());
                            e.printStackTrace();
                            rankedCount = processDummyDigest(digestDate, templateName);
                            digestTitle = "경제뉴스 다이제스트 - " + digestDate + " (더미)";
                            digestBody = generateDummyDigestBody(rankedCount);
                        }
                    } else {
                        // 더미 다이제스트 생성
                        System.out.println("더미 다이제스트 생성 모드");
                        rankedCount = processDummyDigest(digestDate, templateName);
                        digestTitle = "경제뉴스 다이제스트 - " + digestDate + " (더미)";
                        digestBody = generateDummyDigestBody(rankedCount);
                    }
                    
                    // DailyDigest DB 저장
                    try {
                        DailyDigest existingDigest = dailyDigestRepository.findByDigestDate(digestDate).orElse(null);
                        
                        if (existingDigest != null) {
                            // 기존 다이제스트 업데이트
                            existingDigest.setTitle(digestTitle);
                            existingDigest.setBodyMarkdown(digestBody);
                            existingDigest.updateCounts(rankedCount, rankedCount);
                            dailyDigestRepository.save(existingDigest);
                            
                            System.out.println("기존 다이제스트 업데이트: " + digestDate);
                        } else {
                            // 새 다이제스트 생성
                            DailyDigest newDigest = new DailyDigest(digestDate, digestTitle, digestBody);
                            newDigest.updateCounts(rankedCount, rankedCount);
                            dailyDigestRepository.save(newDigest);
                            
                            System.out.println("새 다이제스트 생성: " + digestDate);
                        }
                    } catch (Exception e) {
                        System.err.println("다이제스트 DB 저장 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.RANKED_ARTICLES_COUNT,
                            rankedCount
                    );
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "digestTitle",
                            digestTitle
                    );
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "digestBody",
                            digestBody
                    );
                    
                    System.out.println("S4_RANK_COMPOSE 완료: " + rankedCount + "개 기사 선별 및 다이제스트 조립 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S5_DISPATCH Step - Discord 발송
     */
    @Bean
    public Step step5Dispatch() {
        return stepBuilderFactory.get("S5_DISPATCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S5_DISPATCH: Discord 발송 시작 ===");
                    
                    Integer rankedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.RANKED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    String dryRun = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("dryRun") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("dryRun").toString() : 
                            "true";
                    
                    @SuppressWarnings("unchecked")
                    List<Long> articleIds = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "articleIds",
                            List.class
                    );
                    
                    int dispatchedCount = 0;
                    
                    if ("true".equals(dryRun)) {
                        System.out.println("DRY RUN 모드: 실제 Discord 발송 건너뜀");
                        dispatchedCount = rankedCount != null ? rankedCount : 0;
                    } else {
                        // Discord 설정 확인
                        if (!discordService.isConfigured()) {
                            System.err.println("Discord Webhook URL이 설정되지 않았습니다. DRY RUN 모드로 전환합니다.");
                            dispatchedCount = rankedCount != null ? rankedCount : 0;
                        } else {
                            // 실제 Discord 발송
                            try {
                                // 더미 다이제스트 메시지 생성 (Task 2.2에서 실제 템플릿으로 교체 예정)
                                String digest = createDummyDigest(articleIds, rankedCount);
                                
                                // Discord 발송
                                boolean success = discordService.sendMessage(digest, "EconDigest Bot");
                                
                                if (success) {
                                    dispatchedCount = rankedCount != null ? rankedCount : 0;
                                    System.out.println("Discord 발송 성공: " + dispatchedCount + "개 항목");
                                } else {
                                    System.err.println("Discord 발송 실패");
                                }
                                
                            } catch (Exception e) {
                                System.err.println("Discord 발송 중 오류 발생: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.DISPATCHED_COUNT,
                            dispatchedCount
                    );
                    
                    // 처리 완료 시간 기록
                    long endTime = System.currentTimeMillis();
                    Long startTime = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_START_TIME,
                            Long.class
                    );
                    
                    if (startTime != null) {
                        long processingTime = endTime - startTime;
                        System.out.println("전체 처리 시간: " + (processingTime / 1000.0) + "초");
                        
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.PROCESSING_TIME,
                                processingTime
                        );
                    }
                    
                    System.out.println("S5_DISPATCH 완료: " + dispatchedCount + "개 다이제스트 발송 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    /**
     * 더미 다이제스트 메시지 생성 (Task 2.2에서 실제 템플릿으로 교체 예정)
     */
    private String createDummyDigest(List<Long> articleIds, Integer rankedCount) {
        StringBuilder digest = new StringBuilder();
        
        digest.append("# 📊 경제뉴스 다이제스트\n");
        digest.append("**생성시간**: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        digest.append("**처리된 기사**: ").append(rankedCount != null ? rankedCount : 0).append("개\n\n");
        
        if (articleIds != null && !articleIds.isEmpty()) {
            // 실제 기사 정보 조회 및 표시 (간단한 형태)
            List<Article> articles = articleService.findByIds(articleIds);
            int count = 1;
            for (Article article : articles) {
                if (count > (rankedCount != null ? rankedCount : 5)) break;
                
                digest.append("## ").append(count).append(". ").append(article.getTitle()).append("\n");
                digest.append("**출처**: ").append(article.getSource()).append("\n");
                if (article.getPublishedAt() != null) {
                    digest.append("**발행**: ").append(article.getPublishedAt().format(
                            java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))).append("\n");
                }
                if (article.getUrl() != null) {
                    digest.append("**링크**: ").append(article.getUrl()).append("\n");
                }
                digest.append("\n");
                count++;
            }
        } else {
            digest.append("처리된 기사가 없습니다.\n");
        }
        
        digest.append("---\n");
        digest.append("*Generated by EconDigest Batch System*");
        
        return digest.toString();
    }
}