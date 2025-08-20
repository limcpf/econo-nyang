package com.yourco.econyang.batch.config;

import com.yourco.econyang.batch.util.ExecutionContextUtil;
import com.yourco.econyang.config.RssSourcesConfig;
import com.yourco.econyang.dto.ArticleDto;
import com.yourco.econyang.service.RssFeedService;
import com.yourco.econyang.service.ContentExtractionService;
import com.yourco.econyang.openai.service.OpenAiClient;
import com.yourco.econyang.openai.dto.EconomicSummaryResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring Batch 설정 클래스
 * ECON_DAILY_DIGEST Job과 관련 Step들을 정의합니다.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private EconDigestJobParametersValidator jobParametersValidator;

    @Autowired
    private RssFeedService rssFeedService;

    @Autowired
    private RssSourcesConfig rssSourcesConfig;
    
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
     * S1_FETCH Step - RSS 피드 수집
     */
    @Bean
    public Step step1Fetch() {
        return stepBuilderFactory.get("S1_FETCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("S1_FETCH: RSS 피드 수집 시작");
                    
                    long startTime = System.currentTimeMillis();
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_START_TIME,
                            startTime
                    );
                    
                    // Job Parameters 읽기
                    String targetDate = chunkContext.getStepContext()
                            .getJobParameters()
                            .get(JobParameters.TARGET_DATE) != null ? 
                            chunkContext.getStepContext().getJobParameters().get(JobParameters.TARGET_DATE).toString() : 
                            JobParameters.DEFAULT_TARGET_DATE;
                    
                    Integer maxArticles = chunkContext.getStepContext()
                            .getJobParameters()
                            .get(JobParameters.MAX_ARTICLES) != null ?
                            Integer.valueOf(chunkContext.getStepContext().getJobParameters().get(JobParameters.MAX_ARTICLES).toString()) :
                            JobParameters.DEFAULT_MAX_ARTICLES;
                    
                    System.out.println("Target Date: " + targetDate);
                    System.out.println("Max Articles: " + maxArticles);
                    
                    try {
                        // RSS 소스에서 기사 수집
                        List<ArticleDto> articles = rssFeedService.fetchAllArticles(
                                rssSourcesConfig.getSources(), maxArticles);
                        
                        // 키워드 필터링 적용
                        List<ArticleDto> filteredArticles = rssFeedService.applyFilters(
                                articles, rssSourcesConfig.getFilters());
                        
                        int fetchedCount = filteredArticles.size();
                        
                        // 결과를 ExecutionContext에 저장
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                                fetchedCount
                        );
                        
                        // 수집된 기사 목록도 저장 (다음 Step에서 사용)
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                "fetchedArticles",
                                filteredArticles
                        );
                        
                        System.out.println("S1_FETCH: RSS 피드 수집 완료 - " + fetchedCount + "개 기사 수집");
                        
                        // 수집된 기사 목록 출력 (처음 5개만)
                        int displayCount = Math.min(5, filteredArticles.size());
                        for (int i = 0; i < displayCount; i++) {
                            ArticleDto article = filteredArticles.get(i);
                            System.out.println("  " + (i + 1) + ". [" + article.getSource() + "] " + article.getTitle());
                        }
                        if (filteredArticles.size() > 5) {
                            System.out.println("  ... 외 " + (filteredArticles.size() - 5) + "개 기사");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("RSS 피드 수집 중 오류 발생: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 오류 발생 시에도 0개로 설정하여 다음 Step이 계속 진행되도록 함
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                                0
                        );
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.ERROR_COUNT,
                                1
                        );
                    }
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S2_EXTRACT Step - 본문 추출 및 정제
     */
    @Bean
    public Step step2Extract() {
        return stepBuilderFactory.get("S2_EXTRACT")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("S2_EXTRACT: 본문 추출 및 정제 시작");
                    
                    // 이전 Step에서 수집된 기사 수 확인
                    Integer fetchedCount = ExecutionContextUtil.getIntFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                            0
                    );
                    
                    // 이전 Step에서 수집된 기사 목록 가져오기
                    @SuppressWarnings("unchecked")
                    List<ArticleDto> fetchedArticles = (List<ArticleDto>) ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "fetchedArticles",
                            List.class
                    );
                    
                    System.out.println("이전 단계에서 수집된 기사 수: " + fetchedCount);
                    if (fetchedArticles == null || fetchedArticles.isEmpty()) {
                        System.out.println("추출할 기사가 없습니다.");
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                                0
                        );
                        return RepeatStatus.FINISHED;
                    }
                    
                    System.out.println("본문 추출 시작: " + fetchedArticles.size() + "개 기사");
                    
                    try {
                        // ContentExtractionService를 사용하여 본문 추출
                        List<ArticleDto> extractedArticles = contentExtractionService.extractContents(fetchedArticles);
                        
                        // 추출 성공한 기사 수 계산
                        int extractedCount = (int) extractedArticles.stream()
                                .mapToLong(article -> article.isExtractSuccess() ? 1 : 0)
                                .sum();
                        
                        // 추출 성공률 계산
                        double successRate = contentExtractionService.calculateSuccessRate(extractedArticles);
                        
                        // 결과를 ExecutionContext에 저장
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                                extractedCount
                        );
                        
                        // 추출된 기사 목록도 저장 (다음 Step에서 사용)
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                "extractedArticles",
                                extractedArticles
                        );
                        
                        System.out.println("S2_EXTRACT: 본문 추출 완료 - " + extractedCount + "/" + fetchedArticles.size() + 
                                         "개 기사 추출 성공 (성공률: " + String.format("%.1f", successRate * 100) + "%)");
                        
                        // 추출 성공한 기사 목록 출력 (처음 3개만)
                        int displayCount = Math.min(3, extractedCount);
                        int successShown = 0;
                        for (ArticleDto article : extractedArticles) {
                            if (article.isExtractSuccess() && successShown < displayCount) {
                                String contentPreview = article.getContent();
                                if (contentPreview != null && contentPreview.length() > 100) {
                                    contentPreview = contentPreview.substring(0, 100) + "...";
                                }
                                System.out.println("  " + (successShown + 1) + ". [" + article.getSource() + "] " + 
                                                 article.getTitle() + " - 본문 " + 
                                                 (article.getContent() != null ? article.getContent().length() : 0) + "자");
                                successShown++;
                            }
                        }
                        
                        // 추출 실패한 기사가 있으면 오류 정보 출력
                        int failedCount = fetchedArticles.size() - extractedCount;
                        if (failedCount > 0) {
                            System.out.println("추출 실패한 기사 " + failedCount + "개:");
                            int failedShown = 0;
                            for (ArticleDto article : extractedArticles) {
                                if (!article.isExtractSuccess() && failedShown < 3) {
                                    System.out.println("  - [" + article.getSource() + "] " + article.getTitle() + 
                                                     " (오류: " + article.getExtractError() + ")");
                                    failedShown++;
                                }
                            }
                            if (failedCount > 3) {
                                System.out.println("  ... 외 " + (failedCount - 3) + "개 실패");
                            }
                        }
                        
                    } catch (Exception e) {
                        System.err.println("본문 추출 중 오류 발생: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 오류 발생 시에도 0개로 설정하여 다음 Step이 계속 진행되도록 함
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                                0
                        );
                        ExecutionContextUtil.putToJobContext(
                                chunkContext.getStepContext().getStepExecution(),
                                ExecutionContextUtil.ERROR_COUNT,
                                1
                        );
                    }
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S3_SUMMARIZE_AI Step - AI 요약/해설 생성
     */
    @Bean
    public Step step3SummarizeAi() {
        return stepBuilderFactory.get("S3_SUMMARIZE_AI")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("S3_SUMMARIZE_AI: AI 요약/해설 생성 시작");
                    
                    // Job Parameters에서 useLLM 확인
                    Boolean useLLM = chunkContext.getStepContext()
                            .getJobParameters()
                            .get(JobParameters.USE_LLM) != null ?
                            Boolean.valueOf(chunkContext.getStepContext().getJobParameters().get(JobParameters.USE_LLM).toString()) :
                            JobParameters.DEFAULT_USE_LLM;
                    
                    // 이전 Step에서 추출된 기사 수 확인
                    Integer extractedCount = ExecutionContextUtil.getIntFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            0
                    );
                    
                    // 이전 Step에서 추출된 기사 목록 가져오기
                    @SuppressWarnings("unchecked")
                    List<ArticleDto> extractedArticles = (List<ArticleDto>) ExecutionContextUtil.getFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            "extractedArticles",
                            List.class
                    );
                    
                    System.out.println("Use LLM: " + useLLM);
                    System.out.println("이전 단계에서 추출된 기사 수: " + extractedCount);
                    
                    if (extractedArticles != null && !extractedArticles.isEmpty()) {
                        System.out.println("추출된 기사 중 성공한 기사 목록 확인:");
                        int displayCount = Math.min(3, extractedCount);
                        int successShown = 0;
                        for (ArticleDto article : extractedArticles) {
                            if (article.isExtractSuccess() && successShown < displayCount) {
                                System.out.println("  " + (successShown + 1) + ". [" + article.getSource() + "] " + 
                                                 article.getTitle() + " (본문 " + 
                                                 (article.getContent() != null ? article.getContent().length() : 0) + "자)");
                                successShown++;
                            }
                        }
                    }
                    
                    int summarizedCount = 0;
                    
                    if (useLLM) {
                        if (!openAiClient.isApiAvailable()) {
                            System.out.println("OpenAI API가 사용할 수 없습니다. 요약을 건너뜁니다.");
                        } else {
                            System.out.println("OpenAI API를 사용하여 AI 요약을 시작합니다.");
                            
                            // 추출 성공한 기사들만 AI 요약 처리
                            for (ArticleDto article : extractedArticles) {
                                if (article.isExtractSuccess() && article.getContent() != null && !article.getContent().trim().isEmpty()) {
                                    try {
                                        System.out.println("AI 요약 처리: [" + article.getSource() + "] " + article.getTitle());
                                        
                                        // OpenAI API를 사용하여 경제 뉴스 요약 생성
                                        EconomicSummaryResponse summary = openAiClient.generateEconomicSummary(
                                            article.getTitle(), 
                                            article.getContent()
                                        );
                                        
                                        // ArticleDto에 AI 요약 결과 저장
                                        article.setAiSummary(summary.getSummary());
                                        article.setAiAnalysis(summary.getAnalysis());
                                        article.setImportanceScore(summary.getImportanceScore());
                                        article.setMarketImpact(summary.getMarketImpact());
                                        article.setInvestorInterest(summary.getInvestorInterest());
                                        article.setConfidenceScore(summary.getConfidenceScore());
                                        article.setSummarizedAt(java.time.LocalDateTime.now());
                                        article.setSummarizeSuccess(true);
                                        
                                        summarizedCount++;
                                        
                                        System.out.println("  요약 완료 - 중요도: " + summary.getImportanceScore() + 
                                                         ", 시장영향: " + summary.getMarketImpact() + 
                                                         ", 신뢰도: " + summary.getConfidenceScore());
                                        
                                        // API 호출 간 짧은 대기 (Rate Limit 방지)
                                        Thread.sleep(500);
                                        
                                    } catch (Exception e) {
                                        System.err.println("  AI 요약 실패: " + e.getMessage());
                                        article.setSummarizeError(e.getMessage());
                                        article.setSummarizeSuccess(false);
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("LLM이 비활성화되어 요약을 건너뜁니다.");
                    }
                    
                    // 결과를 ExecutionContext에 저장
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            summarizedCount
                    );
                    
                    System.out.println("S3_SUMMARIZE_AI: AI 요약/해설 생성 완료 (stub) - " + summarizedCount + "개 기사 요약");
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S4_RANK_COMPOSE Step - 중요도 산정 및 다이제스트 구성
     */
    @Bean
    public Step step4RankCompose() {
        return stepBuilderFactory.get("S4_RANK_COMPOSE")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("S4_RANK_COMPOSE: 중요도 산정 및 다이제스트 구성 시작");
                    
                    // 이전 Step에서 요약된 기사 수 확인
                    Integer summarizedCount = ExecutionContextUtil.getIntFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.SUMMARIZED_ARTICLES_COUNT,
                            0
                    );
                    
                    System.out.println("이전 단계에서 요약된 기사 수: " + summarizedCount);
                    
                    // TODO: 중요도 산정 및 다이제스트 구성 로직 구현 (2주차에서)
                    // 임시로 가상의 다이제스트 ID 생성
                    Long digestId = System.currentTimeMillis(); // 임시 ID
                    
                    // 결과를 ExecutionContext에 저장
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FINAL_DIGEST_ID,
                            digestId
                    );
                    
                    System.out.println("S4_RANK_COMPOSE: 중요도 산정 및 다이제스트 구성 완료 (stub) - Digest ID: " + digestId);
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * S5_DISPATCH Step - 다이제스트 발송
     */
    @Bean
    public Step step5Dispatch() {
        return stepBuilderFactory.get("S5_DISPATCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("S5_DISPATCH: 다이제스트 발송 시작");
                    
                    // Job Parameters에서 dryRun 확인
                    Boolean dryRun = chunkContext.getStepContext()
                            .getJobParameters()
                            .get(JobParameters.DRY_RUN) != null ?
                            Boolean.valueOf(chunkContext.getStepContext().getJobParameters().get(JobParameters.DRY_RUN).toString()) :
                            JobParameters.DEFAULT_DRY_RUN;
                    
                    // 이전 Step에서 생성된 다이제스트 ID 확인
                    Long digestId = ExecutionContextUtil.getLongFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FINAL_DIGEST_ID,
                            null
                    );
                    
                    System.out.println("Dry Run: " + dryRun);
                    System.out.println("Digest ID: " + digestId);
                    
                    // TODO: 디스코드 발송 로직 구현 (2주차에서)
                    String dispatchStatus;
                    if (dryRun) {
                        dispatchStatus = "DRY_RUN_SUCCESS";
                        System.out.println("드라이런 모드로 실제 발송하지 않습니다.");
                    } else {
                        dispatchStatus = "PENDING"; // 실제 발송 로직이 구현되면 SUCCESS/FAILED로 설정
                        System.out.println("실제 발송 로직이 아직 구현되지 않았습니다.");
                    }
                    
                    // 결과를 ExecutionContext에 저장
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.DISPATCH_STATUS,
                            dispatchStatus
                    );
                    
                    // 처리 종료 시간 기록
                    long endTime = System.currentTimeMillis();
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_END_TIME,
                            endTime
                    );
                    
                    // 총 처리 시간 계산
                    Long startTime = ExecutionContextUtil.getLongFromJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.PROCESSING_START_TIME,
                            endTime
                    );
                    long processingTime = endTime - startTime;
                    
                    System.out.println("S5_DISPATCH: 다이제스트 발송 완료 (stub) - Status: " + dispatchStatus);
                    System.out.println("총 처리 시간: " + processingTime + "ms");
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}