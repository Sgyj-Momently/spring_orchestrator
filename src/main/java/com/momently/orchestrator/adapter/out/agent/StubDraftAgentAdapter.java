package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.DraftAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 초안 작성 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubDraftAgentAdapter implements DraftAgentPort {

    @Override
    public DraftResult createDraft(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult
    ) {
        return new DraftResult(
            Math.max(1, outlineResult.outlineSectionCount()),
            "output/%s/draft/draft.json".formatted(projectId)
        );
    }
}

