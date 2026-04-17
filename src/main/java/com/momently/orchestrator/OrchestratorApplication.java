package com.momently.orchestrator;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the workflow orchestrator application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OrchestratorApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args runtime arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
