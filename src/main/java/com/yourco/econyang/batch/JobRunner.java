package com.yourco.econyang.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 배치 Job 실행을 위한 CommandLineRunner
 * 개발/테스트 목적으로 사용됩니다.
 */
@Component
public class JobRunner implements CommandLineRunner {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job econDailyDigestJob;
    
    @Override
    public void run(String... args) throws Exception {
        // 개발 환경에서만 자동 실행하도록 설정
        // 실제 운영에서는 스케줄러나 외부 트리거에 의해 실행
        boolean autoRun = false;
        
        // 커맨드라인 인자에서 --batch.auto-run=true가 있는지 확인
        for (String arg : args) {
            if ("--batch.auto-run=true".equals(arg)) {
                autoRun = true;
                break;
            }
        }
        
        if (autoRun) {
            System.out.println("=== ECON_DAILY_DIGEST Job 자동 실행 시작 ===");
            
            // 기본 Job Parameters 설정
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", "yesterday")
                    .addString("maxArticles", "5")
                    .addString("dryRun", "true")
                    .addString("useLLM", "false")
                    .addLong("timestamp", System.currentTimeMillis()) // 유니크한 실행을 위한 타임스탬프
                    .toJobParameters();
            
            try {
                jobLauncher.run(econDailyDigestJob, jobParameters);
                System.out.println("=== ECON_DAILY_DIGEST Job 자동 실행 완료 ===");
            } catch (Exception e) {
                System.err.println("=== ECON_DAILY_DIGEST Job 실행 실패 ===");
                e.printStackTrace();
            }
        } else {
            System.out.println("배치 Job 자동 실행이 비활성화되어 있습니다.");
            System.out.println("실행하려면 --batch.auto-run=true 인자를 추가하세요.");
        }
    }
}