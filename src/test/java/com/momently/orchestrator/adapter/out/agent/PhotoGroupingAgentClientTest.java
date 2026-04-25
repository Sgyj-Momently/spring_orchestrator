package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.config.PhotoGroupingAgentProperties;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 사진 그룹화 HTTP adapter의 요청/응답 계약을 검증한다.
 */
class PhotoGroupingAgentClientTest {

    @Test
    @DisplayName("그룹화 에이전트에 JSON POST 요청을 보내고 결과 요약을 반환한다")
    void postsPayloadAndReturnsGroupingResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            builder
        );
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "grouping_strategy": "LOCATION_BASED"
                }
                """))
            .andRespond(withSuccess("""
                {
                  "grouping_strategy": "LOCATION_BASED",
                  "group_count": 2,
                  "groups": []
                }
                """, MediaType.APPLICATION_JSON));

        PhotoGroupingResult result = client.groupPhotos(Map.of(
            "project_id", "project-001",
            "grouping_strategy", "LOCATION_BASED"
        ));

        assertThat(result.groupingStrategy()).isEqualTo("LOCATION_BASED");
        assertThat(result.groupCount()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("에이전트 호출 실패는 애플리케이션 예외로 감싼다")
    void wrapsAgentCallFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            builder
        );
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.groupPhotos(Map.of(
            "project_id", "project-001",
            "grouping_strategy", "LOCATION_BASED"
        )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to call photo grouping agent");

        server.verify();
    }
}
