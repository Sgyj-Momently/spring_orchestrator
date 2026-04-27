package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.HeroPhotoAgentProperties;
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

class HeroPhotoAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("그룹 결과와 bundle 사진을 대표 사진 에이전트 요청으로 변환하고 결과를 저장한다")
    void postsGroupsAndPhotosThenPersistsResult() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photos": [
                {
                  "file_name": "IMG_0001.jpg",
                  "photo_summary": {
                    "summary": "카페 디저트",
                    "ocr_text": ["cake"],
                    "confidence": 0.92
                  }
                },
                {
                  "file_name": "LICENSE.jpg",
                  "photo_summary": {
                    "exclude_from_public_outputs": true,
                    "summary": "[민감정보 이미지 제외]"
                  }
                }
              ]
            }
            """);
        Path groupingPath = writeGrouping("""
            {
              "group_count": 1,
              "groups": [
                {
                  "group_id": "group-001",
                  "photo_ids": ["file:IMG_0001.jpg"]
                }
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HeroPhotoAgentClient client = new HeroPhotoAgentClient(
            new HeroPhotoAgentProperties("http://hero-photo.test", "/api/v1/hero-photos"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://hero-photo.test/api/v1/hero-photos"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [
                    {
                      "group_id": "group-001",
                      "photo_ids": ["file:IMG_0001.jpg"]
                    }
                  ],
                  "photos": [
                    {
                      "photo_id": "file:IMG_0001.jpg",
                      "file_name": "IMG_0001.jpg",
                      "summary": "카페 디저트",
                      "ocr_text": ["cake"],
                      "confidence": 0.92
                    }
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "hero_photo_count": 1,
                  "hero_photos": [
                    {
                      "group_id": "group-001",
                      "hero_photo_id": "file:IMG_0001.jpg",
                      "reason": "best_confidence_then_richness"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        HeroPhotoResult result = client.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(1, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 1, groupingPath.toString())
        );

        assertThat(result.heroPhotoCount()).isEqualTo(1);
        assertThat(result.resultPath()).isEqualTo(
            tempDir.resolve("output").resolve("hero-photo").resolve("hero-result.json").toString()
        );
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"hero_photo_result\"");
        server.verify();
    }

    @Test
    @DisplayName("대표 사진 에이전트 4xx 응답은 추적 가능한 예외로 감싼다")
    void wrapsHeroPhotoClientError() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": []}");
        Path groupingPath = writeGrouping("{\"groups\": []}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HeroPhotoAgentClient client = new HeroPhotoAgentClient(
            new HeroPhotoAgentProperties("http://hero-photo.test", "/api/v1/hero-photos"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://hero-photo.test/api/v1/hero-photos"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest().body("{\"error\":\"bad groups\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Hero photo agent call failed")
            .hasMessageContaining("status=400")
            .hasMessageContaining("projectId=project-001")
            .hasMessageContaining("bad groups");

        server.verify();
    }

    @Test
    @DisplayName("배열이 아닌 groups/photos artifact는 빈 목록으로 안전하게 변환한다")
    void treatsNonArrayArtifactsAsEmptyLists() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": {}}");
        Path groupingPath = writeGrouping("{\"groups\": {}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HeroPhotoAgentClient client = new HeroPhotoAgentClient(
            new HeroPhotoAgentProperties("http://hero-photo.test", "/api/v1/hero-photos"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://hero-photo.test/api/v1/hero-photos"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [],
                  "photos": []
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "hero_photo_count": 0,
                  "hero_photos": []
                }
                """, MediaType.APPLICATION_JSON));

        HeroPhotoResult result = client.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString())
        );

        assertThat(result.heroPhotoCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("파일명이 없는 사진과 비정상 요약 필드는 대표 사진 요청에서 안전하게 정리한다")
    void sanitizesMalformedPhotoSummaryFields() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photos": [
                {
                  "photo_summary": {
                    "summary": "파일명이 없어 제외"
                  }
                },
                {
                  "file_name": "IMG_0002.jpg",
                  "photo_summary": {
                    "summary": "",
                    "ocr_text": "not-array",
                    "confidence": "high"
                  }
                }
              ]
            }
            """);
        Path groupingPath = writeGrouping("{\"groups\": []}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HeroPhotoAgentClient client = new HeroPhotoAgentClient(
            new HeroPhotoAgentProperties("http://hero-photo.test", "/api/v1/hero-photos"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://hero-photo.test/api/v1/hero-photos"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [],
                  "photos": [
                    {
                      "photo_id": "file:IMG_0002.jpg",
                      "file_name": "IMG_0002.jpg",
                      "summary": null,
                      "ocr_text": [],
                      "confidence": null
                    }
                  ]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "hero_photo_count": 0,
                  "hero_photos": []
                }
                """, MediaType.APPLICATION_JSON));

        HeroPhotoResult result = client.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(1, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString())
        );

        assertThat(result.heroPhotoCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("그룹 결과 파일을 읽을 수 없으면 예외로 전파한다")
    void wrapsGroupingReadFailure() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": []}");
        RestClient.Builder builder = RestClient.builder();
        HeroPhotoAgentClient client = new HeroPhotoAgentClient(
            new HeroPhotoAgentProperties("http://hero-photo.test", "/api/v1/hero-photos"),
            new ObjectMapper(),
            builder
        );

        assertThatThrownBy(() -> client.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, tempDir.resolve("missing.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read grouping result");
    }

    private Path writeBundle(String content) throws IOException {
        Path bundlePath = tempDir.resolve("output").resolve("bundles").resolve("bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, content);
        return bundlePath;
    }

    private Path writeGrouping(String content) throws IOException {
        Path groupingPath = tempDir.resolve("output").resolve("grouping").resolve("grouping-result.json");
        Files.createDirectories(groupingPath.getParent());
        Files.writeString(groupingPath, content);
        return groupingPath;
    }
}
