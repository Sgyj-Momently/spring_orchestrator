package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.OutlineAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 stub-agents 프로필에서 사용하는 개요 생성 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubOutlineAgentAdapter implements OutlineAgentPort {

    @Override
    public OutlineResult createOutline(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult
    ) {
        int sectionCount = Math.max(0, photoGroupingResult.groupCount());
        String resultPath = "output/%s/outline/outline.json".formatted(projectId);
        writeJson(
            Path.of(resultPath),
            """
            {
              "artifact_type": "outline_result",
              "project_id": "%s",
              "section_count": %d,
              "outline": {
                "title": "%s",
                "sections": []
              }
            }
            """.formatted(projectId, sectionCount, projectId)
        );
        return new OutlineResult(sectionCount, resultPath);
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
