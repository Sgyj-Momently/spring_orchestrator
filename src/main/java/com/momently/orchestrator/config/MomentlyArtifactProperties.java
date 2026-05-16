package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 워크플로 산출물과 사용자 편집본 보존 정책 설정이다.
 */
@ConfigurationProperties(prefix = "momently.artifacts")
public record MomentlyArtifactProperties(int maxEditVersions) {

    public MomentlyArtifactProperties {
        if (maxEditVersions < 0) {
            maxEditVersions = 20;
        }
    }
}
