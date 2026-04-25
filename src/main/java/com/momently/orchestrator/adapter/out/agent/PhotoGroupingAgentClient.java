package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.config.PhotoGroupingAgentProperties;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * FastAPI 사진 그룹화 에이전트를 HTTP로 호출하는 outbound adapter다.
 *
 * <p>이 adapter는 {@link PhotoGroupingAgentPort} 구현체로서, application 계층이 FastAPI의 URL,
 * JSON field naming, HTTP client 세부사항에 의존하지 않게 한다. {@code memory} 프로필이 아닐 때
 * 활성화되며, 로컬 개발 stub 대신 실제 그룹화 서버를 호출한다.</p>
 *
 * <p>응답 JSON 전체를 application 계층으로 흘리지 않고, 현재 오케스트레이션에 필요한
 * {@code grouping_strategy}, {@code group_count}만 {@link PhotoGroupingResult}로 변환한다.
 * HTTP 오류나 네트워크 오류는 {@link IllegalStateException}으로 감싸 워크플로 실패 기록으로 이어지게 한다.</p>
 */
@Component
@Profile("!memory")
public class PhotoGroupingAgentClient implements PhotoGroupingAgentPort {

    private final PhotoGroupingAgentProperties properties;
    private final RestClient restClient;

    /**
     * HTTP client와 에이전트 설정을 생성한다.
     *
     * <p>{@code baseUrl}은 {@link PhotoGroupingAgentProperties}에서만 가져온다. 공개 API 요청에서
     * 에이전트 주소를 받지 않으므로, 클라이언트가 내부 인프라 위치를 바꾸는 일을 막을 수 있다.</p>
     *
     * @param properties 그룹화 에이전트 base URL과 endpoint 경로 설정
     * @param restClientBuilder Spring이 제공하는 HTTP client builder
     */
    public PhotoGroupingAgentClient(
        PhotoGroupingAgentProperties properties,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * 그룹화 payload를 FastAPI 에이전트에 POST하고 요약 결과로 변환한다.
     *
     * <p>요청 본문은 Spring 내부에서 만든 snake_case JSON 계약을 그대로 사용한다. FastAPI 응답에
     * {@code groups} 같은 큰 본문이 포함되어도 이 단계에서는 상태 전이에 필요한 요약만 읽는다.
     * 응답 body가 비어 있거나 HTTP 호출이 실패하면 런타임 예외를 던진다.</p>
     *
     * @param payload 그룹화 에이전트의 {@code /api/v1/photo-groups} 계약에 맞춘 요청 본문
     * @return 실제 적용 전략과 생성 그룹 수를 담은 결과
     * @throws IllegalStateException 빈 응답, HTTP 오류, 네트워크 오류가 발생한 경우
     */
    @Override
    public PhotoGroupingResult groupPhotos(Map<String, Object> payload) {
        try {
            PhotoGroupingAgentResponse response = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(PhotoGroupingAgentResponse.class);

            if (response == null) {
                throw new IllegalStateException("Photo grouping agent returned an empty response");
            }
            return response.toResult();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call photo grouping agent", exception);
        }
    }

    /**
     * 그룹화 에이전트 응답 중 오케스트레이터가 즉시 필요로 하는 필드다.
     *
     * <p>FastAPI 응답에는 그룹 목록과 LLM 보정 메타데이터가 더 들어올 수 있다. 이 record는 그 값을
     * 무시하고 application port 결과에 필요한 필드만 안정적으로 역직렬화한다.</p>
     *
     * @param groupingStrategy FastAPI가 실제 적용했다고 응답한 그룹화 전략
     * @param groupCount FastAPI가 생성했다고 응답한 그룹 수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotoGroupingAgentResponse(
        @JsonProperty("grouping_strategy") String groupingStrategy,
        @JsonProperty("group_count") int groupCount
    ) {

        /**
         * HTTP 응답 DTO를 application port 결과로 변환한다.
         *
         * @return application 계층이 사용하는 그룹화 요약 결과
         */
        PhotoGroupingResult toResult() {
            return new PhotoGroupingResult(groupingStrategy, groupCount);
        }
    }
}
