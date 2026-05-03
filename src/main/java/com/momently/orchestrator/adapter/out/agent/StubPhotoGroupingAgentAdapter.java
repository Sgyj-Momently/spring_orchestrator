package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 stub-agents 프로필에서 사용하는 사진 그룹화 에이전트 대역이다.
 *
 * <p>이 어댑터는 로컬 개발에서 Spring 오케스트레이터의 상태 전이와 웹 API를 먼저 검증하기 위해
 * 존재한다. 외부 FastAPI 서버나 bundle 아티팩트를 읽지 않으며, 전달받은 전략 값을 그대로 결과로 돌려준다.
 * 실제 그룹 생성 결과가 아니므로 운영 프로필에서는 사용하지 않는다.</p>
 */
@Component
@Profile("stub-agents")
public class StubPhotoGroupingAgentAdapter implements PhotoGroupingAgentPort {

    /**
     * 워크플로의 {@code groupingStrategy} 값만 읽어 그룹화 결과 요약을 만든다.
     *
     * <p>외부 호출이나 파일 I/O가 없는 deterministic 대역 구현이다. {@code photoInfoResult}는
     * 실제 어댑터와 같은 포트 계약을 유지하기 위한 입력이며 여기서는 사용하지 않는다. 그룹 수는 실제 그룹화가
     * 수행되지 않았음을 나타내기 위해 0으로 둔다.</p>
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @param groupingStrategy 워크플로 생성 시 선택된 그룹화 전략
     * @param photoInfoResult 사진 정보 추출 단계가 남긴 bundle 아티팩트 참조
     * @return 응답에 담긴 전략 값과 0개 그룹 수를 담은 대역 결과
     */
    @Override
    public PhotoGroupingResult groupPhotos(
        String projectId,
        String groupingStrategy,
        int timeWindowMinutes,
        PhotoInfoResult photoInfoResult
    ) {
        String resultPath = "artifacts/photo-grouping/%s/grouping-result.json".formatted(projectId);
        Path path = Path.of(resultPath);
        writeJson(
            path,
            """
            {
              "artifact_type": "photo_grouping_result",
              "project_id": "%s",
              "grouping_strategy": "%s",
              "time_window_minutes": %d,
              "groups": []
            }
            """.formatted(projectId, groupingStrategy, timeWindowMinutes)
        );
        return new PhotoGroupingResult(groupingStrategy, 0, resultPath);
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
