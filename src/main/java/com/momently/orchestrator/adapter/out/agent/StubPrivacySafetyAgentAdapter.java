package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.PrivacySafetyAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * stub-agents 프로필에서 사용하는 민감정보 안전성 검사 에이전트 대역이다.
 */
@Component
@Profile("stub-agents")
public class StubPrivacySafetyAgentAdapter implements PrivacySafetyAgentPort {

    @Override
    public PrivacySafetyResult reviewPrivacy(String projectId, PhotoInfoResult photoInfoResult) {
        Path resultPath = Path.of("output/%s/privacy/privacy-result.json".formatted(projectId));
        writeJson(
            resultPath,
            """
            {
              "artifact_type": "privacy_safety_result",
              "project_id": "%s",
              "public_photo_count": %d,
              "excluded_photo_count": 0,
              "public_photos": [],
              "excluded_photos": []
            }
            """.formatted(projectId, photoInfoResult.photoCount())
        );
        return new PrivacySafetyResult(
            photoInfoResult.photoCount(),
            0,
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

