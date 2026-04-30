package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 최종 검수 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubReviewAgentAdapter implements ReviewAgentPort {

    @Override
    public ReviewResult reviewDocument(String projectId, PhotoInfoResult photoInfoResult, StyleResult styleResult) {
        return new ReviewResult(0, "output/%s/review/final.json".formatted(projectId));
    }
}

