package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import com.momently.orchestrator.config.QualityScoreAgentProperties;
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

class QualityScoreAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("bundle photos를 quality 요청으로 변환하고 scored bundle을 저장한다")
    void postsQualityPayloadAndPersistsArtifacts() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photo_count": 1,
              "photos": [
                {"file_name": "IMG_0001.jpg", "summary": "bright beach scene"}
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QualityScoreAgentClient client = new QualityScoreAgentClient(
            new QualityScoreAgentProperties("http://quality.test", "/api/v1/quality-scores"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://quality.test/api/v1/quality-scores"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "photos": [
                    {"file_name": "IMG_0001.jpg", "summary": "bright beach scene"}
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "quality_status": "ok",
                  "photo_count": 1,
                  "average_score": 0.82,
                  "scored_photos": [
                    {
                      "file_name": "IMG_0001.jpg",
                      "quality_bucket": "high",
                      "quality_score": {"overall": 0.82}
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        QualityScoreResult result = client.scorePhotos("project-001", new PhotoInfoResult(1, bundlePath.toString()));

        assertThat(result.scoredPhotoCount()).isEqualTo(1);
        assertThat(result.averageScore()).isEqualTo(0.82);
        assertThat(result.resultPath()).isEqualTo(tempDir.resolve("output/quality/quality-result.json").toString());
        assertThat(result.scoredBundlePath()).isEqualTo(tempDir.resolve("output/quality/bundle.json").toString());
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("quality_score_result");
        assertThat(Files.readString(Path.of(result.scoredBundlePath())))
            .contains("\"photo_count\" : 1")
            .contains("\"average_quality_score\" : 0.82")
            .contains("\"quality_bucket\" : \"high\"");
        server.verify();
    }

    @Test
    @DisplayName("photos가 배열이 아니면 빈 배열로 요청한다")
    void treatsNonArrayPhotosAsEmptyList() throws IOException {
        Path bundlePath = writeBundle("{\"photo_count\":0,\"photos\":{}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QualityScoreAgentClient client = new QualityScoreAgentClient(
            new QualityScoreAgentProperties("http://quality.test", "/api/v1/quality-scores"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://quality.test/api/v1/quality-scores"))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "photos": []
                }
                """))
            .andRespond(withSuccess("""
                {
                  "photo_count": 0,
                  "average_score": 0.0,
                  "scored_photos": []
                }
                """, MediaType.APPLICATION_JSON));

        QualityScoreResult result = client.scorePhotos("project-001", new PhotoInfoResult(0, bundlePath.toString()));

        assertThat(result.scoredPhotoCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("quality 에이전트 서버 오류는 추적 가능한 예외로 감싼다")
    void wrapsServerFailure() throws IOException {
        Path bundlePath = writeBundle("{\"photo_count\":0,\"photos\":[]}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QualityScoreAgentClient client = new QualityScoreAgentClient(
            new QualityScoreAgentProperties("http://quality.test", "/api/v1/quality-scores"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://quality.test/api/v1/quality-scores"))
            .andRespond(withServerError().body("{\"error\":\"down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.scorePhotos("project-001", new PhotoInfoResult(0, bundlePath.toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Quality score agent call failed")
            .hasMessageContaining("down");
        server.verify();
    }

    @Test
    @DisplayName("bundle artifact를 읽을 수 없으면 예외로 전파한다")
    void wrapsBundleReadFailure() {
        QualityScoreAgentClient client = new QualityScoreAgentClient(
            new QualityScoreAgentProperties("http://quality.test", "/api/v1/quality-scores"),
            new ObjectMapper(),
            RestClient.builder()
        );

        assertThatThrownBy(() -> client.scorePhotos(
            "project-001",
            new PhotoInfoResult(0, tempDir.resolve("missing.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");
    }

    @Test
    @DisplayName("stub quality adapter는 입력 bundle을 그대로 downstream bundle로 반환한다")
    void stubReturnsInputBundle() {
        StubQualityScoreAgentAdapter adapter = new StubQualityScoreAgentAdapter();

        QualityScoreResult result = adapter.scorePhotos("project-001", new PhotoInfoResult(3, "bundle.json"));

        assertThat(result.scoredPhotoCount()).isEqualTo(3);
        assertThat(result.averageScore()).isEqualTo(0.75);
        assertThat(result.scoredBundlePath()).isEqualTo("bundle.json");
    }

    private Path writeBundle(String content) throws IOException {
        Path bundlePath = tempDir.resolve("output/bundles/bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, content);
        return bundlePath;
    }
}
