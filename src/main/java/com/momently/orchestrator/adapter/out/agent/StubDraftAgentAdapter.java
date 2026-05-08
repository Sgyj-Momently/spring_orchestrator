package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.DraftAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
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
 * stub-agents 프로필에서 사용하는 초안 작성 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubDraftAgentAdapter implements DraftAgentPort {

    @Override
    public DraftResult createDraft(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult,
        String voiceProfileId
    ) {
        Path path = Path.of("output/%s/draft/draft.json".formatted(projectId));
        int sectionCount = Math.max(1, outlineResult.outlineSectionCount());
        writeJson(
            path,
            """
            {
              "artifact_type": "draft_result",
              "project_id": "%s",
              "section_count": %d,
              "markdown": ""
            }
            """.formatted(projectId, sectionCount)
        );
        return new DraftResult(
            sectionCount,
            path.toString()
        );
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

