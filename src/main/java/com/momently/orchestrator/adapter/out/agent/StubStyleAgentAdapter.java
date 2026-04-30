package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.StyleAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 문체 적용 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubStyleAgentAdapter implements StyleAgentPort {

    @Override
    public StyleResult applyStyle(String projectId, DraftResult draftResult) {
        return new StyleResult(120, "output/%s/style/styled.json".formatted(projectId));
    }
}

