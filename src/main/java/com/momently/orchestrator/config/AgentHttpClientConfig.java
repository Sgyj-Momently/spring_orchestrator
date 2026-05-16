package com.momently.orchestrator.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * 에이전트 HTTP 호출용 RestClient 공통 설정이다.
 */
@Configuration(proxyBeanMethods = false)
public class AgentHttpClientConfig {

    @Bean
    RestClientCustomizer agentRestClientTimeoutCustomizer(AgentHttpClientProperties properties) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
            requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
            builder.requestFactory(requestFactory);
        };
    }
}
