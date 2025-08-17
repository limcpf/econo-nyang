package com.yourco.econdigest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
@EnableBatchProcessing
public class EconDigestApplication {

    public static void main(String[] args) {
        SpringApplication.run(EconDigestApplication.class, args);
    }

}