package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;

/**
 * 초안 작성(draft) 단계를 애플리케이션 계층에서 호출하기 위한 아웃바운드 포트다.
 */
public interface DraftAgentPort {

    DraftResult createDraft(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult,
        String voiceProfileId
    );
}

