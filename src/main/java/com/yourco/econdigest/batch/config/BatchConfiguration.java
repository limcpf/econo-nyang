package com.yourco.econdigest.batch.config;

import com.yourco.econdigest.batch.util.ExecutionContextUtil;
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
                    
                    // TODO: RSS 피드 수집 로직 구현 (Task 4에서)
                    // 임시로 가상의 데이터 생성
                    int fetchedCount = Math.min(maxArticles, 8); // 가상의 수집된 기사 수
                    
                    // 결과를 ExecutionContext에 저장
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.FETCHED_ARTICLES_COUNT,
                            fetchedCount
                    );
                    
                    System.out.println("S1_FETCH: RSS 피드 수집 완료 (stub) - " + fetchedCount + "개 기사 수집");
                    
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
                    
                    System.out.println("이전 단계에서 수집된 기사 수: " + fetchedCount);
                    
                    // TODO: 본문 추출 로직 구현 (Task 5에서)
                    // 임시로 가상의 처리 결과 생성 (일부 기사는 추출 실패할 수 있음)
                    int extractedCount = (int) (fetchedCount * 0.8); // 80% 성공률 가정
                    
                    // 결과를 ExecutionContext에 저장
                    ExecutionContextUtil.putToJobContext(
                            chunkContext.getStepContext().getStepExecution(),
                            ExecutionContextUtil.EXTRACTED_ARTICLES_COUNT,
                            extractedCount
                    );
                    
                    System.out.println("S2_EXTRACT: 본문 추출 및 정제 완료 (stub) - " + extractedCount + "개 기사 추출");
                    
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
                    
                    System.out.println("Use LLM: " + useLLM);
                    System.out.println("이전 단계에서 추출된 기사 수: " + extractedCount);
                    
                    // TODO: AI 요약 로직 구현 (Task 7에서)
                    // 임시로 가상의 처리 결과 생성
                    int summarizedCount;
                    if (useLLM) {
                        summarizedCount = extractedCount; // LLM 사용 시 모든 기사 요약
                    } else {
                        summarizedCount = 0; // LLM 미사용 시 요약 없음
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