package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.momently.orchestrator.config.DraftAgentProperties;
import com.momently.orchestrator.config.ReviewAgentProperties;
import com.momently.orchestrator.config.StyleAgentProperties;
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

class DraftStyleReviewAgentClientTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("draft 클라이언트는 outline 결과를 초안 요청으로 변환하고 결과를 저장한다")
    void draftClientPostsPayloadAndPersistsResult() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[{\"file_name\":\"IMG.jpg\"}]}");
        Path grouping = write("output/grouping/grouping-result.json", "{\"groups\":[{\"group_id\":\"g1\"}]}");
        Path hero = write("output/hero-photo/hero-result.json", "{\"hero_photos\":[{\"group_id\":\"g1\"}]}");
        Path outline = write("output/outline/outline.json", "{\"outline\":{\"title\":\"Trip\",\"sections\":[{\"heading\":\"Day\"}]}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DraftAgentClient client = new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://draft.test/api/v1/drafts"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "outline": {"title": "Trip", "sections": [{"heading": "Day"}]},
                  "groups": [{"group_id": "g1"}],
                  "hero_photos": [{"group_id": "g1"}],
                  "photos": [{"file_name": "IMG.jpg"}]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "draft_status": "ok",
                  "section_count": 1,
                  "markdown": "# Trip"
                }
                """, MediaType.APPLICATION_JSON));

        DraftResult result = client.createDraft(
            "project-001",
            new PhotoInfoResult(1, bundle.toString()),
            new PhotoGroupingResult("LOCATION_BASED", 1, grouping.toString()),
            new HeroPhotoResult(1, hero.toString()),
            new OutlineResult(1, outline.toString()),
            null
        );

        assertThat(result.draftSectionCount()).isEqualTo(1);
        assertThat(result.resultPath()).isEqualTo(tempDir.resolve("output/draft/draft.json").toString());
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"draft_result\"");
        server.verify();
    }

    @Test
    @DisplayName("style 클라이언트는 draft markdown을 style 요청으로 변환하고 결과를 저장한다")
    void styleClientPostsPayloadAndPersistsResult() throws IOException {
        Path draft = write("output/draft/draft.json", "{\"markdown\":\"# Trip\\n\\nBody\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StyleAgentClient client = new StyleAgentClient(
            new StyleAgentProperties("http://style.test", "/api/v1/styles"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://style.test/api/v1/styles"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "draft_markdown": "# Trip\\n\\nBody",
                  "style": "warm_blog"
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "style_status": "ok",
                  "word_count": 4,
                  "markdown": "# Trip"
                }
                """, MediaType.APPLICATION_JSON));

        StyleResult result = client.applyStyle("project-001", new DraftResult(1, draft.toString()), null);

        assertThat(result.wordCount()).isEqualTo(4);
        assertThat(result.resultPath()).isEqualTo(tempDir.resolve("output/style/styled.json").toString());
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"style_result\"");
        server.verify();
    }

    @Test
    @DisplayName("review 클라이언트는 styled markdown과 제외 사진을 검수 요청으로 변환하고 결과를 저장한다")
    void reviewClientPostsPayloadAndPersistsResult() throws IOException {
        Path bundle = write(
            "output/bundles/bundle.json",
            "{\"photos\":[{\"file_name\":\"IMG.jpg\"}],\"excluded_photos\":[{\"file_name\":\"secret.jpg\"}]}"
        );
        Path style = write("output/style/styled.json", "{\"markdown\":\"# Trip\\n\\nBody\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReviewAgentClient client = new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://review.test/api/v1/reviews"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "styled_markdown": "# Trip\\n\\nBody",
                  "photos": [{"file_name": "IMG.jpg"}],
                  "excluded_photos": [{"file_name": "secret.jpg"}]
                }
                """))
            .andRespond(withSuccess("""
                {
                  "project_id": "project-001",
                  "review_status": "ok",
                  "issue_count": 0,
                  "final_markdown": "# Trip"
                }
                """, MediaType.APPLICATION_JSON));

        ReviewResult result = client.reviewDocument(
            "project-001",
            new PhotoInfoResult(1, bundle.toString()),
            new StyleResult(4, style.toString())
        );

        assertThat(result.issueCount()).isZero();
        assertThat(result.resultPath()).isEqualTo(tempDir.resolve("output/review/final.json").toString());
        assertThat(Files.readString(Path.of(result.resultPath()))).contains("\"artifact_type\" : \"review_result\"");
        server.verify();
    }

    @Test
    @DisplayName("배열이 아닌 downstream artifact 필드는 빈 배열로 보낸다")
    void treatsNonArrayDownstreamArtifactsAsEmptyArrays() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":{},\"excluded_photos\":{}}");
        Path grouping = write("output/grouping/grouping-result.json", "{\"groups\":{}}");
        Path hero = write("output/hero-photo/hero-result.json", "{\"hero_photos\":{}}");
        Path outline = write("output/outline/outline.json", "{\"outline\":{\"sections\":[]}}");
        RestClient.Builder draftBuilder = RestClient.builder();
        MockRestServiceServer draftServer = MockRestServiceServer.bindTo(draftBuilder).build();
        DraftAgentClient draftClient = new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            new ObjectMapper(),
            draftBuilder
        );
        draftServer.expect(requestTo("http://draft.test/api/v1/drafts"))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "groups": [],
                  "hero_photos": [],
                  "photos": []
                }
                """))
            .andRespond(withSuccess("{\"section_count\":0,\"markdown\":\"\"}", MediaType.APPLICATION_JSON));

        DraftResult draft = draftClient.createDraft(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new PhotoGroupingResult("TIME_BASED", 0, grouping.toString()),
            new HeroPhotoResult(0, hero.toString()),
            new OutlineResult(0, outline.toString()),
            null
        );
        assertThat(draft.draftSectionCount()).isZero();
        draftServer.verify();

        Path style = write("output/style/styled.json", "{\"markdown\":\"# Trip\"}");
        RestClient.Builder reviewBuilder = RestClient.builder();
        MockRestServiceServer reviewServer = MockRestServiceServer.bindTo(reviewBuilder).build();
        ReviewAgentClient reviewClient = new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            new ObjectMapper(),
            reviewBuilder
        );
        reviewServer.expect(requestTo("http://review.test/api/v1/reviews"))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "photos": [],
                  "excluded_photos": []
                }
                """))
            .andRespond(withSuccess("{\"issue_count\":0,\"final_markdown\":\"# Trip\"}", MediaType.APPLICATION_JSON));

        ReviewResult review = reviewClient.reviewDocument(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new StyleResult(1, style.toString())
        );
        assertThat(review.issueCount()).isZero();
        reviewServer.verify();
    }

    @Test
    @DisplayName("빈 HTTP 응답은 단계별 클라이언트에서 명확한 예외가 된다")
    void rejectsEmptyHttpResponses() throws IOException {
        Path draft = write("output/draft/draft.json", "{\"markdown\":\"# Trip\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StyleAgentClient client = new StyleAgentClient(
            new StyleAgentProperties("http://style.test", "/api/v1/styles"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://style.test/api/v1/styles"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.applyStyle("project-001", new DraftResult(1, draft.toString()), null))
            .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    @DisplayName("HTTP 200 이라도 draft_status 가 ok 가 아니면 실패로 본다")
    void rejectsDraftOkHttpWithErrorStatus() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[]}");
        Path grouping = write("output/grouping/grouping-result.json", "{\"groups\":[]}");
        Path hero = write("output/hero-photo/hero-result.json", "{\"hero_photos\":[]}");
        Path outline = write("output/outline/outline.json", "{\"outline\":{\"sections\":[]}}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DraftAgentClient client = new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://draft.test/api/v1/drafts"))
            .andRespond(withSuccess(
                "{\"draft_status\":\"error: llm_failed\",\"section_count\":0,\"markdown\":\"\"}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createDraft(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new PhotoGroupingResult("TIME_BASED", 0, grouping.toString()),
            new HeroPhotoResult(0, hero.toString()),
            new OutlineResult(0, outline.toString()),
            null
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("draft_status is not ok");
        server.verify();
    }

    @Test
    @DisplayName("HTTP 200 이라도 style_status 가 ok 로 시작하지 않으면 실패로 본다")
    void rejectsStyleOkHttpWithErrorStatus() throws IOException {
        Path draft = write("output/draft/draft.json", "{\"markdown\":\"# Trip\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StyleAgentClient client = new StyleAgentClient(
            new StyleAgentProperties("http://style.test", "/api/v1/styles"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://style.test/api/v1/styles"))
            .andRespond(withSuccess(
                "{\"style_status\":\"error: ollama down\",\"word_count\":1,\"markdown\":\"#\"}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.applyStyle("project-001", new DraftResult(1, draft.toString()), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("style_status");
        server.verify();
    }

    @Test
    @DisplayName("HTTP 200 이라도 알 수 없는 review_status 면 실패로 본다")
    void rejectsReviewOkHttpWithUnknownStatus() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[]}");
        Path style = write("output/style/styled.json", "{\"markdown\":\"# Trip\\n\\nBody text ok\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReviewAgentClient client = new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://review.test/api/v1/reviews"))
            .andRespond(withSuccess(
                "{\"review_status\":\"failed\",\"issue_count\":0,\"final_markdown\":\"# Trip\"}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.reviewDocument(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new StyleResult(2, style.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("review_status is unexpected");
        server.verify();
    }

    @Test
    @DisplayName("needs_attention 검수 결과는 허용한다")
    void allowsReviewNeedsAttention() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[]}");
        Path style = write("output/style/styled.json", "{\"markdown\":\"# Trip\\n\\nBody text ok\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReviewAgentClient client = new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://review.test/api/v1/reviews"))
            .andRespond(withSuccess(
                "{\"review_status\":\"needs_attention\",\"issue_count\":2,\"final_markdown\":\"# Trip\"}",
                MediaType.APPLICATION_JSON));

        ReviewResult result = client.reviewDocument(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new StyleResult(2, style.toString())
        );
        assertThat(result.issueCount()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("새 클라이언트들은 서버 오류를 추적 가능한 예외로 감싼다")
    void wrapsServerFailures() throws IOException {
        Path draft = write("output/draft/draft.json", "{\"markdown\":\"# Trip\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        StyleAgentClient client = new StyleAgentClient(
            new StyleAgentProperties("http://style.test", "/api/v1/styles"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://style.test/api/v1/styles"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError().body("{\"error\":\"down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.applyStyle("project-001", new DraftResult(1, draft.toString()), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Style agent call failed")
            .hasMessageContaining("status=500")
            .hasMessageContaining("down");
        server.verify();
    }

    @Test
    @DisplayName("draft/review 클라이언트는 서버 오류를 추적 가능한 예외로 감싼다")
    void wrapsDraftAndReviewServerFailures() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[]}");
        Path grouping = write("output/grouping/grouping-result.json", "{\"groups\":[]}");
        Path hero = write("output/hero-photo/hero-result.json", "{\"hero_photos\":[]}");
        Path outline = write("output/outline/outline.json", "{\"outline\":{\"sections\":[]}}");
        RestClient.Builder draftBuilder = RestClient.builder();
        MockRestServiceServer draftServer = MockRestServiceServer.bindTo(draftBuilder).build();
        DraftAgentClient draftClient = new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            new ObjectMapper(),
            draftBuilder
        );
        draftServer.expect(requestTo("http://draft.test/api/v1/drafts"))
            .andRespond(withServerError().body("{\"error\":\"draft down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> draftClient.createDraft(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new PhotoGroupingResult("TIME_BASED", 0, grouping.toString()),
            new HeroPhotoResult(0, hero.toString()),
            new OutlineResult(0, outline.toString()),
            null
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Draft agent call failed")
            .hasMessageContaining("draft down");
        draftServer.verify();

        Path style = write("output/style/styled.json", "{\"markdown\":\"# Trip\"}");
        RestClient.Builder reviewBuilder = RestClient.builder();
        MockRestServiceServer reviewServer = MockRestServiceServer.bindTo(reviewBuilder).build();
        ReviewAgentClient reviewClient = new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            new ObjectMapper(),
            reviewBuilder
        );
        reviewServer.expect(requestTo("http://review.test/api/v1/reviews"))
            .andRespond(withServerError().body("{\"error\":\"review down\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> reviewClient.reviewDocument(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new StyleResult(1, style.toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Review agent call failed")
            .hasMessageContaining("review down");
        reviewServer.verify();
    }

    @Test
    @DisplayName("outline artifact가 null이면 빈 outline 계약으로 draft 요청을 보낸다")
    void sendsFallbackOutlineWhenOutlineArtifactIsNull() throws IOException {
        Path bundle = write("output/bundles/bundle.json", "{\"photos\":[]}");
        Path grouping = write("output/grouping/grouping-result.json", "{\"groups\":[]}");
        Path hero = write("output/hero-photo/hero-result.json", "{\"hero_photos\":[]}");
        Path outline = write("output/outline/outline.json", "{\"outline\":null}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DraftAgentClient client = new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            new ObjectMapper(),
            builder
        );
        server.expect(requestTo("http://draft.test/api/v1/drafts"))
            .andExpect(content().json("""
                {
                  "project_id": "project-001",
                  "outline": {"title": "project-001", "sections": []}
                }
                """))
            .andRespond(withSuccess("{\"section_count\":1,\"markdown\":\"# project-001\"}", MediaType.APPLICATION_JSON));

        DraftResult result = client.createDraft(
            "project-001",
            new PhotoInfoResult(0, bundle.toString()),
            new PhotoGroupingResult("TIME_BASED", 0, grouping.toString()),
            new HeroPhotoResult(0, hero.toString()),
            new OutlineResult(0, outline.toString()),
            null
        );

        assertThat(result.draftSectionCount()).isEqualTo(1);
        server.verify();
    }

    @Test
    @DisplayName("artifact 파일을 읽을 수 없으면 단계별 클라이언트가 예외로 전파한다")
    void wrapsArtifactReadFailures() {
        ObjectMapper objectMapper = new ObjectMapper();
        assertThatThrownBy(() -> new DraftAgentClient(
            new DraftAgentProperties("http://draft.test", "/api/v1/drafts"),
            objectMapper,
            RestClient.builder()
        ).createDraft(
            "project-001",
            new PhotoInfoResult(0, tempDir.resolve("missing-bundle.json").toString()),
            new PhotoGroupingResult("TIME_BASED", 0, tempDir.resolve("missing-grouping.json").toString()),
            new HeroPhotoResult(0, tempDir.resolve("missing-hero.json").toString()),
            new OutlineResult(0, tempDir.resolve("missing-outline.json").toString()),
            null
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");

        assertThatThrownBy(() -> new StyleAgentClient(
            new StyleAgentProperties("http://style.test", "/api/v1/styles"),
            objectMapper,
            RestClient.builder()
        ).applyStyle("project-001", new DraftResult(0, tempDir.resolve("missing-draft.json").toString()), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");

        assertThatThrownBy(() -> new ReviewAgentClient(
            new ReviewAgentProperties("http://review.test", "/api/v1/reviews"),
            objectMapper,
            RestClient.builder()
        ).reviewDocument(
            "project-001",
            new PhotoInfoResult(0, tempDir.resolve("missing-bundle.json").toString()),
            new StyleResult(0, tempDir.resolve("missing-style.json").toString())
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read json artifact");
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
