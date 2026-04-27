package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.OutlineAgentProperties;
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

class OutlineAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("그룹/대표사진/bundle을 outline 에이전트 요청으로 변환하고 결과를 저장한다")
    void postsOutlinePayloadAndPersistsResult() throws IOException {
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
                }
              ]
            }
            """);
        Path groupingPath = writeGrouping("""
            {
              "groups": [
                {
                  "group_id": "group-001",
                  "photo_ids": ["file:IMG_0001.jpg"]
                }
              ]
            }
            """);
        Path heroPath = writeHero("""
            {
              "hero_photos": [
                {
                  "group_id": "group-001",
                  "hero_photo_id": "file:IMG_0001.jpg",
                  "reason": "best_confidence_then_richness"
                }
              ]
            }
            """);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutlineAgentClient client = new OutlineAgentClient(
            new OutlineAgentProperties("http://outline.test", "/api/v1/outlines"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://outline.test/api/v1/outlines"))
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
                  "hero_photos": [
                    {
                      "group_id": "group-001",
                      "hero_photo_id": "file:IMG_0001.jpg",
                      "reason": "best_confidence_then_richness"
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
                  "outline_status": "ok",
                  "outline": {
                    "title": "카페 기록",
                    "sections": [
                      {
                        "section_id": "s1",
                        "heading": "입구",
                        "bullets": ["디저트"],
                        "supporting_photo_ids": ["file:IMG_0001.jpg"]
                      }
                    ]
                  }
                }
                """, MediaType.APPLICATION_JSON));

        OutlineResult result = client.createOutline(
            "project-001",
            new PhotoInfoResult(1, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 1, groupingPath.toString()),
            new HeroPhotoResult(1, heroPath.toString())
        );

        assertThat(result.outlineSectionCount()).isEqualTo(1);
        assertThat(result.resultPath()).isEqualTo(
            tempDir.resolve("output").resolve("outline").resolve("outline.json").toString()
        );
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"outline_result\"");
        server.verify();
    }

    @Test
    @DisplayName("outline 에이전트 호출 실패는 추적 가능한 예외로 감싼다")
    void wrapsOutlineAgentFailure() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": []}");
        Path groupingPath = writeGrouping("{\"groups\": []}");
        Path heroPath = writeHero("{\"hero_photos\": []}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutlineAgentClient client = new OutlineAgentClient(
            new OutlineAgentProperties("http://outline.test", "/api/v1/outlines"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://outline.test/api/v1/outlines"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError().body("{\"error\":\"ollama down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createOutline(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString()),
            new HeroPhotoResult(0, heroPath.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Outline agent call failed")
            .hasMessageContaining("status=500")
            .hasMessageContaining("projectId=project-001")
            .hasMessageContaining("ollama down");

        server.verify();
    }

    @Test
    @DisplayName("배열이 아닌 groups/hero/photos artifact는 빈 목록으로 안전하게 변환한다")
    void treatsNonArrayArtifactsAsEmptyLists() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": {}}");
        Path groupingPath = writeGrouping("{\"groups\": {}}");
        Path heroPath = writeHero("{\"hero_photos\": {}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutlineAgentClient client = new OutlineAgentClient(
            new OutlineAgentProperties("http://outline.test", "/api/v1/outlines"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://outline.test/api/v1/outlines"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [],
                  "hero_photos": [],
                  "photos": []
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "outline_status": "ok",
                  "outline": null
                }
                """, MediaType.APPLICATION_JSON));

        OutlineResult result = client.createOutline(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString()),
            new HeroPhotoResult(0, heroPath.toString())
        );

        assertThat(result.outlineSectionCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("파일명이 없거나 공개 제외된 사진은 outline 요청에서 제외한다")
    void sanitizesOutlinePhotos() throws IOException {
        Path bundlePath = writeBundle("""
            {
              "photos": [
                {
                  "photo_summary": {
                    "summary": "파일명이 없어 제외"
                  }
                },
                {
                  "file_name": "LICENSE.jpg",
                  "photo_summary": {
                    "exclude_from_public_outputs": true,
                    "summary": "[민감정보 이미지 제외]"
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
        Path heroPath = writeHero("{\"hero_photos\": []}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OutlineAgentClient client = new OutlineAgentClient(
            new OutlineAgentProperties("http://outline.test", "/api/v1/outlines"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://outline.test/api/v1/outlines"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [],
                  "hero_photos": [],
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
                  "outline_status": "ok",
                  "outline": {
                    "sections": []
                  }
                }
                """, MediaType.APPLICATION_JSON));

        OutlineResult result = client.createOutline(
            "project-001",
            new PhotoInfoResult(1, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString()),
            new HeroPhotoResult(0, heroPath.toString())
        );

        assertThat(result.outlineSectionCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("대표 사진 결과를 읽을 수 없으면 예외로 전파한다")
    void wrapsHeroResultReadFailure() throws IOException {
        Path bundlePath = writeBundle("{\"photos\": []}");
        Path groupingPath = writeGrouping("{\"groups\": []}");
        RestClient.Builder builder = RestClient.builder();
        OutlineAgentClient client = new OutlineAgentClient(
            new OutlineAgentProperties("http://outline.test", "/api/v1/outlines"),
            new ObjectMapper(),
            builder
        );

        assertThatThrownBy(() -> client.createOutline(
            "project-001",
            new PhotoInfoResult(0, bundlePath.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 0, groupingPath.toString()),
            new HeroPhotoResult(0, tempDir.resolve("missing-hero.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");
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

    private Path writeHero(String content) throws IOException {
        Path heroPath = tempDir.resolve("output").resolve("hero-photo").resolve("hero-result.json");
        Files.createDirectories(heroPath.getParent());
        Files.writeString(heroPath, content);
        return heroPath;
    }
}
