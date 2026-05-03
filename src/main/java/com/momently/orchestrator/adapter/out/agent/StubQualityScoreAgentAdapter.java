package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.QualityScoreAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 사진 품질 점수화 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubQualityScoreAgentAdapter implements QualityScoreAgentPort {

    @Override
    public QualityScoreResult scorePhotos(String projectId, PhotoInfoResult photoInfoResult) {
        Path resultPath = Path.of("output/%s/quality/quality-result.json".formatted(projectId));
        writeJson(
            resultPath,
            """
            {
              "artifact_type": "quality_score_result",
              "project_id": "%s",
              "photo_count": %d,
              "average_score": 0.75,
              "scored_photos": []
            }
            """.formatted(projectId, photoInfoResult.photoCount())
        );
        return new QualityScoreResult(
            photoInfoResult.photoCount(),
            0.75,
            resultPath.toString(),
            photoInfoResult.bundlePath()
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

