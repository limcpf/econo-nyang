package com.yourco.econyang.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 환경을 위한 데이터베이스 설정
 * H2 데이터베이스와 PostgreSQL의 호환성을 해결
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {
    
    // H2 환경에서는 PostgreSQL 특화 기능을 비활성화
    // JSON, Array 타입들은 문자열로 저장/조회
}