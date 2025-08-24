package com.yourco.econyang.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 전용 배치 설정
 * 복잡한 데이터베이스 의존성 없이 기본 배치 플로우만 테스트
 */
@TestConfiguration
@Profile("test")
public class TestBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean("testJob")
    @Primary
    public Job testJob() {
        return jobBuilderFactory.get("TestJob")
                .incrementer(new RunIdIncrementer())
                .start(testStep())
                .build();
    }

    @Bean
    public Step testStep() {
        return stepBuilderFactory.get("S1_FETCH")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("=== Test Step 실행 ===");
                    
                    // Job Parameters 확인
                    String dryRun = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("dryRun") != null ? 
                            chunkContext.getStepContext().getJobParameters().get("dryRun").toString() : 
                            "true";
                    
                    System.out.println("DryRun: " + dryRun);
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}