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
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import com.momently.orchestrator.config.PrivacySafetyAgentProperties;
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

class PrivacySafetyAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("bundle photos를 privacy 요청으로 변환하고 sanitized bundle을 저장한다")
    void postsPrivacyPayloadAndPersistsArtifacts() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photo_count": 2,
              "photos": [
                {"file_name": "IMG_0001.jpg"},
                {"file_name": "passport.jpg"}
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PrivacySafetyAgentClient client = new PrivacySafetyAgentClient(
            new PrivacySafetyAgentProperties("http://privacy.test", "/api/v1/privacy-safety"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://privacy.test/api/v1/privacy-safety"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "photos": [
                    {"file_name": "IMG_0001.jpg"},
                    {"file_name": "passport.jpg"}
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "privacy_status": "ok",
                  "public_photo_count": 1,
                  "excluded_photo_count": 1,
                  "public_photos": [
                    {"file_name": "IMG_0001.jpg"}
                  ],
                  "excluded_photos": [
                    {"file_name": "passport.jpg", "exclusion_reason": "sensitive_filename"}
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        PrivacySafetyResult result = client.reviewPrivacy("project-001", new PhotoInfoResult(2, bundlePath.toString()));

        assertThat(result.publicPhotoCount()).isEqualTo(1);
        assertThat(result.excludedPhotoCount()).isEqualTo(1);
        assertThat(result.resultPath()).isEqualTo(tempDir.resolve("output/privacy/privacy-result.json").toString());
        assertThat(result.sanitizedBundlePath()).isEqualTo(tempDir.resolve("output/privacy/bundle.json").toString());
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("privacy_safety_result");
        assertThat(Files.readString(Path.of(result.sanitizedBundlePath())))
            .contains("\"photo_count\" : 1")
            .contains("IMG_0001.jpg")
            .contains("passport.jpg");
        server.verify();
    }

    @Test
    @DisplayName("photos가 배열이 아니면 빈 배열로 요청한다")
    void treatsNonArrayPhotosAsEmptyList() throws IOException {
        Path bundlePath = writeBundle("{\"photo_count\":0,\"photos\":{}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PrivacySafetyAgentClient client = new PrivacySafetyAgentClient(
            new PrivacySafetyAgentProperties("http://privacy.test", "/api/v1/privacy-safety"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://privacy.test/api/v1/privacy-safety"))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "photos": []
                }
                """))
            .andRespond(withSuccess("""
                {
                  "public_photo_count": 0,
                  "excluded_photo_count": 0,
                  "public_photos": [],
                  "excluded_photos": []
                }
                """, MediaType.APPLICATION_JSON));

        PrivacySafetyResult result = client.reviewPrivacy("project-001", new PhotoInfoResult(0, bundlePath.toString()));

        assertThat(result.publicPhotoCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("privacy 에이전트 서버 오류는 추적 가능한 예외로 감싼다")
    void wrapsServerFailure() throws IOException {
        Path bundlePath = writeBundle("{\"photo_count\":0,\"photos\":[]}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PrivacySafetyAgentClient client = new PrivacySafetyAgentClient(
            new PrivacySafetyAgentProperties("http://privacy.test", "/api/v1/privacy-safety"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://privacy.test/api/v1/privacy-safety"))
            .andRespond(withServerError().body("{\"error\":\"down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.reviewPrivacy("project-001", new PhotoInfoResult(0, bundlePath.toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Privacy safety agent call failed")
            .hasMessageContaining("down");
        server.verify();
    }

    @Test
    @DisplayName("bundle artifact를 읽을 수 없으면 예외로 전파한다")
    void wrapsBundleReadFailure() {
        PrivacySafetyAgentClient client = new PrivacySafetyAgentClient(
            new PrivacySafetyAgentProperties("http://privacy.test", "/api/v1/privacy-safety"),
            new ObjectMapper(),
            RestClient.builder()
        );

        assertThatThrownBy(() -> client.reviewPrivacy(
            "project-001",
            new PhotoInfoResult(0, tempDir.resolve("missing.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");
    }

    @Test
    @DisplayName("stub privacy adapter는 입력 bundle을 그대로 downstream bundle로 반환한다")
    void stubReturnsInputBundle() {
        StubPrivacySafetyAgentAdapter adapter = new StubPrivacySafetyAgentAdapter();

        PrivacySafetyResult result = adapter.reviewPrivacy("project-001", new PhotoInfoResult(3, "bundle.json"));

        assertThat(result.publicPhotoCount()).isEqualTo(3);
        assertThat(result.excludedPhotoCount()).isZero();
        assertThat(result.sanitizedBundlePath()).isEqualTo("bundle.json");
    }

    private Path writeBundle(String content) throws IOException {
        Path bundlePath = tempDir.resolve("output/bundles/bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, content);
        return bundlePath;
    }
}

