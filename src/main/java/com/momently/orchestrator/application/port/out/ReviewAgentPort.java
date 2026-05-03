package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;

/**
 * 최종 검수(review) 단계를 애플리케이션 계층에서 호출하기 위한 아웃바운드 포트다.
 */
public interface ReviewAgentPort {

    ReviewResult reviewDocument(String projectId, PhotoInfoResult photoInfoResult, StyleResult styleResult);
}

