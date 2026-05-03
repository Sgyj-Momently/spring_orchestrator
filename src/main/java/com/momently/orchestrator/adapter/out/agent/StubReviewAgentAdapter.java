package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 최종 검수 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubReviewAgentAdapter implements ReviewAgentPort {

    @Override
    public ReviewResult reviewDocument(String projectId, PhotoInfoResult photoInfoResult, StyleResult styleResult) {
        Path resultPath = Path.of("output/%s/review/final.json".formatted(projectId));
        writeJson(
            resultPath,
            """
            {
              "artifact_type": "review_result",
              "project_id": "%s",
              "issue_count": 0,
              "issues": []
            }
            """.formatted(projectId)
        );
        return new ReviewResult(0, resultPath.toString());
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

