package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;

/**
 * 민감정보 안전성 검사 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 */
public interface PrivacySafetyAgentPort {

    PrivacySafetyResult reviewPrivacy(String projectId, PhotoInfoResult photoInfoResult);
}

