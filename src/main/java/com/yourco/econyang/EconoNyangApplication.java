package com.yourco.econyang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

/**
 * EconoNyang - 매일 너에게 경제뉴스를 소개해주는 귀여운 친구 🐱
 * 
 * 스프링 배치를 통해 매일 아침 7:30에 
 * 디스코드로 경제뉴스 다이제스트를 전달합니다.
 */
@SpringBootApplication
@EnableBatchProcessing
public class EconoNyangApplication {

    public static void main(String[] args) {
        SpringApplication.run(EconoNyangApplication.class, args);
    }

}