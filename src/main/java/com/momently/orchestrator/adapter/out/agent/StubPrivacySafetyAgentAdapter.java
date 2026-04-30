package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.PrivacySafetyAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 민감정보 안전성 검사 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubPrivacySafetyAgentAdapter implements PrivacySafetyAgentPort {

    @Override
    public PrivacySafetyResult reviewPrivacy(String projectId, PhotoInfoResult photoInfoResult) {
        return new PrivacySafetyResult(
            photoInfoResult.photoCount(),
            0,
            "output/%s/privacy/privacy-result.json".formatted(projectId),
            photoInfoResult.bundlePath()
        );
    }
}

