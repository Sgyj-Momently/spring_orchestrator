package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 stub-agents 프로필에서 사용하는 대표 사진 선택 에이전트 대역이다.
 *
 * <p>이 어댑터는 외부 서버나 아티팩트 파일을 읽지 않고, 오케스트레이터의 상태 전이와 API를
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
        writeJson(
            Path.of(resultPath),
            """
            {
              "artifact_type": "hero_photo_result",
              "project_id": "%s",
              "hero_photo_count": %d,
              "hero_photos": []
            }
            """.formatted(projectId, heroPhotoCount)
        );
        return new HeroPhotoResult(heroPhotoCount, resultPath);
    }

    private static void writeJson(Path path, String json) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write stub artifact: " + path, exception);
        }
    }
}

