package com.yourco.econyang.batch.config;

import com.yourco.econyang.batch.util.ExecutionContextUtil;
import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.service.ArticleService;
import com.yourco.econyang.service.ContentExtractionService;
import com.yourco.econyang.service.RssFeedService;

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
     * S2_EXTRACT Step - 본문 추출 및 정제 (DB 기반)
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
                    
                    // 본문 추출 (현재는 더미 처리)
                    int extractedCount = 0;
                    for (Article article : articles) {
                        if (article.getContent() == null) {
                            // 더미 본문 설정 (Task 2.1에서 실제 추출 구현 예정)
                            article.setContent("더미 본문 내용: " + article.getTitle() + "에 대한 상세한 기사 내용입니다. 경제 동향과 관련된 중요한 정보가 포함되어 있습니다.");
                            article.setExtractedAt(java.time.LocalDateTime.now());
                            articleService.save(article);
                            extractedCount++;
                        }
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
     * S3_SUMMARIZE_AI Step - AI 요약 생성 (더미)
     */
    @Bean
    public Step step3SummarizeAi() {
        return stepBuilderFactory.get("S3_SUMMARIZE_AI")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S3_SUMMARIZE_AI: AI 요약 생성 시작 (더미 모드) ===");
                    
                    Integer extractedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    int summarizedCount = extractedCount != null ? extractedCount : 0;
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            summarizedCount
                    );
                    
                    System.out.println("S3_SUMMARIZE_AI 완료: " + summarizedCount + "개 기사 요약 완료 (더미)");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S4_RANK_COMPOSE Step - 중요도 산정 및 다이제스트 조립 (더미)
     */
    @Bean
    public Step step4RankCompose() {
        return stepBuilderFactory.get("S4_RANK_COMPOSE")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S4_RANK_COMPOSE: 중요도 산정 및 다이제스트 조립 시작 (더미 모드) ===");
                    
                    Integer summarizedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    // 상위 5개만 선별한 것으로 가정
                    int rankedCount = Math.min(summarizedCount != null ? summarizedCount : 0, 5);
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.RANKED_ARTICLES_COUNT,
                            rankedCount
                    );
                    
                    System.out.println("S4_RANK_COMPOSE 완료: " + rankedCount + "개 기사 선별 및 다이제스트 조립 완료 (더미)");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S5_DISPATCH Step - Discord 발송 (더미)
     */
    @Bean
    public Step step5Dispatch() {
        return stepBuilderFactory.get("S5_DISPATCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S5_DISPATCH: Discord 발송 시작 (더미 모드) ===");
                    
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
                    
                    if ("true".equals(dryRun)) {
                        System.out.println("DRY RUN 모드: 실제 Discord 발송 건너뜀");
                    } else {
                        System.out.println("Discord 발송 완료 (더미)");
                    }
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.DISPATCHED_COUNT,
                            rankedCount != null ? rankedCount : 0
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
                    
                    System.out.println("S5_DISPATCH 완료: " + (rankedCount != null ? rankedCount : 0) + "개 다이제스트 발송 완료 (더미)");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}