package com.momently.orchestrator.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * JWT 보안 필터 체인이 실제 빈 조합과 함께 동작하는지 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureObservability(metrics = true)
@ActiveProfiles({"memory", "stub-agents"})
class OrchestratorApiSecurityIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 업로드 API가 존재하는 입력 루트(임시 디렉터리)다. */
    private static volatile Path pipelineInputRoot;

    @DynamicPropertySource
    static void registerPhotoPipelineInputRoot(DynamicPropertyRegistry registry) {
        registry.add(
            "agents.photo-info.pipeline.input-root",
            () -> {
                try {
                    if (pipelineInputRoot == null) {
                        pipelineInputRoot = Files.createTempDirectory("orc-photo-input-root");
                        pipelineInputRoot.toFile().deleteOnExit();
                    }
                    return pipelineInputRoot.toAbsolutePath().toString();
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Prometheus 노출(actuator/prometheus)은 인증 없이 가능하다")
    void exposesPrometheusForScrape() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("# HELP")));
    }

    @Test
    @DisplayName("토큰 없으면 워크플로 API에 401")
    void deniesWorkflowWithoutJwt() throws Exception {
        UUID id = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0faa");
        mockMvc.perform(get("/api/v1/workflows/{workflowId}", id))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("/api 밖 요청은 denyAll로 접근 불가 메시지")
    void deniesNonApiRequest() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("올바른 로그인 뒤 보호 경로 접근 허용")
    void workflowCreateWithJwtSucceeds() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"console","password":"changeme"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn();

        JsonNode body = MAPPER.readTree(login.getResponse().getContentAsString());
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresInSeconds").asLong()).isPositive();
        String token = body.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("""
                    {"projectId":"p1","groupingStrategy":"LOCATION_BASED","timeWindowMinutes":90}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowId").exists());
    }

    @Test
    @DisplayName("틀린 비번은 로그인 401")
    void loginFailsOnBadCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"console","password":"no"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("JWT가 있어야 업로드 API 사용 가능하다")
    void uploadDeniedWithoutJwt() throws Exception {
        byte[] png = tinyPng();
        MockMultipartFile part = new MockMultipartFile("files", "a.png", "image/png", png);
        mockMvc.perform(multipart("/api/v1/uploads/images").file(part))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 후 PNG 업로드가 성공한다")
    void uploadWithJwtStoresFile() throws Exception {
        JsonNode login = MAPPER.readTree(
            loginOk().getResponse().getContentAsString());
        byte[] png = tinyPng();

        MockMultipartFile part = new MockMultipartFile(
            "files",
            "a.png",
            "image/png",
            png);

        mockMvc.perform(multipart("/api/v1/uploads/images")
                .file(part)
                .header("Authorization", "Bearer " + login.get("accessToken").asText()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.savedCount").value(1))
            .andExpect(jsonPath("$.bytesTotal").value(png.length));
    }

    private MvcResult loginOk() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"console","password":"changeme"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
    }

    private static byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }
}
