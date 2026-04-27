package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.PhotoGroupingAgentProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 사진 그룹화 HTTP adapter의 요청/응답 계약을 검증한다.
 */
class PhotoGroupingAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("bundle 사진 목록을 그룹화 에이전트 JSON 요청으로 변환하고 결과 요약을 반환한다")
    void postsBundlePhotosAndReturnsGroupingResult() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photo_count": 1,
              "source_photo_count": 2,
              "excluded_photo_count": 1,
              "photos": [
                {
                  "file_name": "LICENSE.jpg",
                  "captured_at": null,
                  "has_gps": false,
                  "gps": null,
                  "photo_summary": {
                    "summary": "[민감정보 이미지 제외]",
                    "exclude_from_public_outputs": true,
                    "privacy": {
                      "sensitive": true,
                      "reasons": ["identity_document"],
                      "action": "exclude_from_public_outputs"
                    }
                  }
                },
                {
                  "file_name": "IMG_0001.jpg",
                  "captured_at": "2026-04-10T09:00:00",
                  "has_gps": true,
                  "gps": { "lat": 35.681, "lon": 139.767 },
                  "photo_summary": {
                    "location_hint": "도쿄역",
                    "scene_type": "city",
                    "summary": "도쿄역 앞 거리 풍경",
                    "subjects": ["station", "people"]
                  }
                }
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "grouping_strategy": "LOCATION_BASED",
                  "photos": [
                    {
                      "photo_id": "file:IMG_0001.jpg",
                      "file_name": "IMG_0001.jpg",
                      "captured_at": "2026-04-10T09:00:00",
                      "has_gps": true,
                      "gps": { "lat": 35.681, "lon": 139.767 },
                      "location_hint": "도쿄역",
                      "scene_type": "city",
                      "summary": "도쿄역 앞 거리 풍경",
                      "subjects": ["station", "people"]
                    }
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "grouping_strategy": "LOCATION_BASED",
                  "group_count": 2,
                  "groups": []
                }
                """, MediaType.APPLICATION_JSON));

        PhotoGroupingResult result = client.groupPhotos(
            "project-001",
            "LOCATION_BASED",
            90,
            new PhotoInfoResult(1, bundlePath.toString())
        );

        assertThat(result.groupingStrategy()).isEqualTo("LOCATION_BASED");
        assertThat(result.groupCount()).isEqualTo(2);
        assertThat(result.resultPath()).isEqualTo(
            tempDir.resolve("output").resolve("grouping").resolve("grouping-result.json").toString()
        );
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"photo_grouping_result\"");
        server.verify();
    }

    @Test
    @DisplayName("에이전트 호출 실패는 애플리케이션 예외로 감싼다")
    void wrapsAgentCallFailure() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );
        Path bundlePath = writeBundle("""
            {
              "photo_count": 0,
              "photos": []
            }
            """);
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.groupPhotos(
            "project-001",
            "LOCATION_BASED",
            90,
            new PhotoInfoResult(0, bundlePath.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Photo grouping agent call failed")
            .hasMessageContaining("status=500")
            .hasMessageContaining("projectId=project-001")
            .hasMessageContaining("groupingStrategy=LOCATION_BASED")
            .hasMessageContaining("photoCount=0");

        server.verify();
    }

    @Test
    @DisplayName("4xx 오류는 상태코드와 응답 바디를 포함해 추적 가능하게 감싼다")
    void wrapsClientErrorWithResponseBody() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );
        Path bundlePath = writeBundle("""
            {
              "photo_count": 1,
              "photos": [
                {
                  "file_name": "IMG_0001.jpg",
                  "captured_at": "2026-04-10T09:00:00",
                  "has_gps": true,
                  "gps": { "lat": 35.681, "lon": 139.767 },
                  "photo_summary": {
                    "location_hint": "도쿄역",
                    "scene_type": "city",
                    "summary": "도쿄역 앞 거리 풍경",
                    "subjects": ["station", "people"]
                  }
                }
              ]
            }
            """);
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest().body("""
                {
                  "error_code": "INVALID_GROUPING_STRATEGY",
                  "message": "grouping_strategy must be one of the allowed enum values"
                }
                """).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.groupPhotos(
            "project-001",
            "NOT_AN_ENUM",
            90,
            new PhotoInfoResult(1, bundlePath.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Photo grouping agent call failed")
            .hasMessageContaining("status=400")
            .hasMessageContaining("projectId=project-001")
            .hasMessageContaining("groupingStrategy=NOT_AN_ENUM")
            .hasMessageContaining("photoCount=1")
            .hasMessageContaining("INVALID_GROUPING_STRATEGY");

        server.verify();
    }

    @Test
    @DisplayName("bundle 파일을 읽을 수 없으면 애플리케이션 예외로 전파한다")
    void wrapsBundleReadFailure() {
        RestClient.Builder builder = RestClient.builder();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );

        assertThatThrownBy(() -> client.groupPhotos(
            "project-001",
            "LOCATION_BASED",
            90,
            new PhotoInfoResult(0, tempDir.resolve("missing-bundle.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read photo info bundle");
    }

    @Test
    @DisplayName("bundle photos가 배열이 아니면 빈 사진 목록으로 그룹화 요청을 보낸다")
    void treatsNonArrayPhotosAsEmptyList() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photo_count": 0,
              "photos": {}
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "grouping_strategy": "LOCATION_BASED",
                  "photos": []
                }
                """))
            .andRespond(withSuccess("""
                {
                  "grouping_strategy": "LOCATION_BASED",
                  "group_count": 0,
                  "groups": []
                }
                """, MediaType.APPLICATION_JSON));

        PhotoGroupingResult result = client.groupPhotos(
            "project-001",
            "LOCATION_BASED",
            90,
            new PhotoInfoResult(0, bundlePath.toString())
        );

        assertThat(result.groupCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("파일명이 없는 사진은 deterministic fallback ID로 변환한다")
    void usesFallbackPhotoNameWhenFileNameMissing() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photo_count": 1,
              "photos": [
                {
                  "captured_at": null,
                  "has_gps": null,
                  "gps": null,
                  "photo_summary": {
                    "subjects": "not-array"
                  }
                }
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PhotoGroupingAgentClient client = new PhotoGroupingAgentClient(
            new PhotoGroupingAgentProperties("http://photo-grouping.test", "/api/v1/photo-groups"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://photo-grouping.test/api/v1/photo-groups"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "grouping_strategy": "LOCATION_BASED",
                  "photos": [
                    {
                      "photo_id": "file:photo-001",
                      "file_name": "photo-001",
                      "captured_at": null,
                      "has_gps": null,
                      "gps": null,
                      "subjects": []
                    }
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "grouping_strategy": "LOCATION_BASED",
                  "group_count": 1,
                  "groups": []
                }
                """, MediaType.APPLICATION_JSON));

        PhotoGroupingResult result = client.groupPhotos(
            "project-001",
            "LOCATION_BASED",
            90,
            new PhotoInfoResult(1, bundlePath.toString())
        );

        assertThat(result.groupCount()).isEqualTo(1);
        server.verify();
    }

    private Path writeBundle(String content) throws IOException {
        Path bundlePath = tempDir.resolve("output").resolve("bundles").resolve("bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, content);
        return bundlePath;
    }
}
