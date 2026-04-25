package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 memory 프로필에서 사용하는 사진 그룹화 에이전트 대역이다.
 *
 * <p>이 adapter는 로컬 개발에서 Spring 오케스트레이터의 상태 전이와 웹 API를 먼저 검증하기 위해
 * 존재한다. 외부 FastAPI 서버를 호출하지 않으며, payload에 담긴 전략 값을 그대로 결과로 돌려준다.
 * 실제 그룹 생성 결과가 아니므로 운영 프로필에서는 사용하지 않는다.</p>
 */
@Component
@Profile("memory")
public class StubPhotoGroupingAgentAdapter implements PhotoGroupingAgentPort {

    /**
     * payload의 {@code grouping_strategy} 값만 읽어 그룹화 결과 요약을 만든다.
     *
     * <p>외부 호출이나 파일 I/O가 없는 deterministic 대역 구현이다. 그룹 수는 실제 그룹화가
     * 수행되지 않았음을 나타내기 위해 0으로 둔다.</p>
     *
     * @param payload 워크플로 러너가 만든 그룹화 요청 payload
     * @return payload 전략 값과 0개 그룹 수를 담은 대역 결과
     */
    @Override
    public PhotoGroupingResult groupPhotos(Map<String, Object> payload) {
        String groupingStrategy = String.valueOf(payload.get("grouping_strategy"));
        return new PhotoGroupingResult(groupingStrategy, 0);
    }
}
