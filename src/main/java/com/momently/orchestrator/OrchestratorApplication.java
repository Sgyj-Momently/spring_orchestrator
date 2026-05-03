package com.momently.orchestrator;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 워크플로 오케스트레이터 애플리케이션의 Spring Boot 진입점이다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class OrchestratorApplication {

    /**
     * Spring Boot 애플리케이션을 기동한다.
     *
     * @param args JVM 실행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
