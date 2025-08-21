package com.yourco.econyang.batch.config;

import com.yourco.econyang.batch.util.ExecutionContextUtil;
import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.service.ContentExtractionService;
import com.yourco.econyang.service.RssFeedService;
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
     * S1_FETCH Step - RSS 피드 수집 (더미)
     */
    @Bean
    public Step step1Fetch() {
        return stepBuilderFactory.get("S1_FETCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S1_FETCH: RSS 피드 수집 시작 (더미 모드) ===");
                    
                    long startTime = System.currentTimeMillis();
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_START_TIME,
                            startTime
                    );
                    
                    // 더미 데이터: 10개 기사를 수집한 것으로 가정
                    int mockFetchedCount = 10;
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                            mockFetchedCount
                    );
                    
                    System.out.println("S1_FETCH 완료: " + mockFetchedCount + "개 기사 수집 완료 (더미)");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S2_EXTRACT Step - 본문 추출 및 정제 (더미)
     */
    @Bean
    public Step step2Extract() {
        return stepBuilderFactory.get("S2_EXTRACT")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== S2_EXTRACT: 본문 추출 및 정제 시작 (더미 모드) ===");
                    
                    Integer fetchedCount = ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                            Integer.class
                    );
                    
                    int extractedCount = fetchedCount != null ? fetchedCount : 0;
                    
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            extractedCount
                    );
                    
                    System.out.println("S2_EXTRACT 완료: " + extractedCount + "개 기사 추출 완료 (더미)");
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