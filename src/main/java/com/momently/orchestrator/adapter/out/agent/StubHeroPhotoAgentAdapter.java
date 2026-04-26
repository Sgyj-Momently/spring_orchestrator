package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 stub-agents 프로필에서 사용하는 대표 사진 선택 에이전트 대역이다.
 *
 * <p>이 adapter는 외부 서버나 artifact 파일을 읽지 않고, 오케스트레이터의 상태 전이와 API를
 * 먼저 검증하기 위해 존재한다. 그룹 수를 대표 사진 수로 간주해 deterministic 결과를 만든다.</p>
 */
@Component
@Profile("stub-agents")
public class StubHeroPhotoAgentAdapter implements HeroPhotoAgentPort {

    @Override
    public HeroPhotoResult selectHeroPhotos(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult
    ) {
        int heroPhotoCount = Math.max(0, photoGroupingResult.groupCount());
        String resultPath = "output/%s/hero-photo/hero-result.json".formatted(projectId);
        return new HeroPhotoResult(heroPhotoCount, resultPath);
    }
}

