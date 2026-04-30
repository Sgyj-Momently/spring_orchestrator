package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.QualityScoreAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 사진 품질 점수화 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubQualityScoreAgentAdapter implements QualityScoreAgentPort {

    @Override
    public QualityScoreResult scorePhotos(String projectId, PhotoInfoResult photoInfoResult) {
        return new QualityScoreResult(
            photoInfoResult.photoCount(),
            0.75,
            "output/%s/quality/quality-result.json".formatted(projectId),
            photoInfoResult.bundlePath()
        );
    }
}

