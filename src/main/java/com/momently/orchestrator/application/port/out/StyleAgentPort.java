package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;

/**
 * 문체 적용(style) 단계를 애플리케이션 계층에서 호출하기 위한 아웃바운드 포트다.
 */
public interface StyleAgentPort {

    StyleResult applyStyle(String projectId, DraftResult draftResult, String voiceProfileId);
}
