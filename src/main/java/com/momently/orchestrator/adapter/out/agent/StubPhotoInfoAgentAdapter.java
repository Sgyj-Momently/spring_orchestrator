package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 실제 HTTP 연동 전까지 memory 프로필에서 사용하는 사진 정보 에이전트 대역이다.
 *
 * <p>이 adapter는 사진 폴더, Ollama, Python 파이프라인 없이도 오케스트레이터의 생성/실행 API와
 * 상태 머신을 확인하기 위한 개발용 구현이다. 실제 bundle 파일을 만들지 않고, 후속 단계가 참조할
 * 수 있는 규칙적인 artifact 경로만 계산한다.</p>
 */
@Component
@Profile("memory")
public class StubPhotoInfoAgentAdapter implements PhotoInfoAgentPort {

    /**
     * 프로젝트 식별자를 기준으로 가상의 bundle artifact 경로를 반환한다.
     *
     * <p>실제 사진 분석을 하지 않으므로 사진 수는 0이다. 운영 실행이나 end-to-end 검증에서는
     * {@link LocalPhotoInfoPipelineAdapter} 같은 실제 adapter를 사용해야 한다.</p>
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @return 대역 bundle 경로와 0개 사진 수를 담은 결과
     */
    @Override
    public PhotoInfoResult extractPhotoInfo(String projectId) {
        String bundlePath = "artifacts/photo-info/%s/bundle.json".formatted(projectId);
        return new PhotoInfoResult(0, bundlePath);
    }
}
