package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;

/**
 * 개요 생성(outline) 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 */
public interface OutlineAgentPort {

    /**
     * 그룹화 결과와 대표 사진 선택 결과를 바탕으로 문서 개요를 생성한다.
     */
    OutlineResult createOutline(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult
    );
}

