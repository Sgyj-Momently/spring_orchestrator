package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;

/**
 * 사진 품질 점수화 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 */
public interface QualityScoreAgentPort {

    QualityScoreResult scorePhotos(String projectId, PhotoInfoResult photoInfoResult);
}

