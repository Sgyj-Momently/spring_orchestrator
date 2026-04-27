package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.OutlineAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 stub-agents 프로필에서 사용하는 개요 생성 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubOutlineAgentAdapter implements OutlineAgentPort {

    @Override
    public OutlineResult createOutline(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult
    ) {
        int sectionCount = Math.max(0, photoGroupingResult.groupCount());
        String resultPath = "output/%s/outline/outline.json".formatted(projectId);
        return new OutlineResult(sectionCount, resultPath);
    }
}
