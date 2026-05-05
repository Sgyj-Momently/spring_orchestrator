package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.DraftAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.DraftAgentProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!stub-agents")
public class DraftAgentClient implements DraftAgentPort {

    private final DraftAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DraftAgentClient(DraftAgentProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public DraftResult createDraft(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult,
        String voiceProfileId
    ) {
        return createDraft(
            projectId,
            photoInfoResult,
            photoGroupingResult,
            heroPhotoResult,
            outlineResult,
            voiceProfileId,
            null,
            null
        );
    }

    @Override
    public DraftResult createDraft(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult,
        String voiceProfileId,
        String contentType,
        String writingInstructions
    ) {
        Path outlinePath = Path.of(outlineResult.resultPath());
        Path resultPath = siblingStagePath(outlinePath, "draft", "draft.json");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("content_type", contentType);
        payload.put("writing_instructions", writingInstructions);
        JsonNode outline = readJson(outlinePath).path("outline");
        payload.put("outline", outline.isObject() ? outline : Map.of("title", projectId, "sections", java.util.List.of()));
        payload.put("groups", readField(Path.of(photoGroupingResult.resultPath()), "groups"));
        payload.put("hero_photos", readField(Path.of(heroPhotoResult.resultPath()), "hero_photos"));
        payload.put("photos", readField(Path.of(photoInfoResult.bundlePath()), "photos"));
        if (voiceProfileId != null && !voiceProfileId.isBlank()) {
            loadVoiceProfile(voiceProfileId).ifPresent(profile -> payload.put("voice_profile", profile));
        }

        JsonNode body = post(payload);
        requireDraftSemanticOk(body);
        writeResult(resultPath, body, "draft_result");
        DraftAgentResponse response = convert(body, DraftAgentResponse.class);
        return new DraftResult(response.sectionCount(), resultPath.toString());
    }

    /** 에이전트가 HTTP 200 과 함께 본문 에러를 담는 패턴을 차단한다 */
    private static void requireDraftSemanticOk(JsonNode body) {
        JsonNode n = body.get("draft_status");
        if (n == null || n.isNull() || n.asText("").isBlank()) {
            return;
        }
        String s = n.asText("").strip();
        if (!"ok".equals(s)) {
            throw new IllegalStateException("Draft agent draft_status is not ok: " + s);
        }
    }

    private JsonNode post(Map<String, Object> payload) {
        try {
            JsonNode body = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw new IllegalStateException("Draft agent returned an empty response");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Draft agent call failed. status=%s, responseBody=%s"
                .formatted(exception.getRawStatusCode(), exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call draft agent: " + exception.getMessage(), exception);
        }
    }

    private Optional<JsonNode> loadVoiceProfile(String voiceProfileId) {
        for (Path root : java.util.List.of(
            // docker compose: momently-voice-data -> /var/lib/momently-voice (voice_profile_agent의 VOICE_PROFILE_DATA_DIR)
            Path.of("/var/lib/momently-voice"),
            // local/dev fallback
            Path.of("../voice_profiles"),
            Path.of("voice_profiles")
        )) {
            Path profilePath = root.resolve(voiceProfileId).resolve("profile.json").normalize();
            if (Files.exists(profilePath)) {
                return Optional.of(readJson(profilePath));
            }
        }
        return Optional.empty();
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read json artifact: " + path, exception);
        }
    }

    private Object readField(Path path, String field) {
        JsonNode node = readJson(path).path(field);
        return node.isArray() ? objectMapper.convertValue(node, Object.class) : java.util.List.of();
    }

    private void writeResult(Path path, JsonNode body, String artifactType) {
        try {
            Files.createDirectories(path.getParent());
            JsonNode persisted = body;
            if (body instanceof ObjectNode objectNode) {
                persisted = objectNode.deepCopy();
                ((ObjectNode) persisted).put("artifact_type", artifactType);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist draft result: " + path, exception);
        }
    }

    private <T> T convert(JsonNode body, Class<T> type) {
        try {
            return objectMapper.treeToValue(body, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse draft response", exception);
        }
    }

    private Path siblingStagePath(Path path, String stage, String fileName) {
        Path stageDirectory = path.getParent();
        Path projectDirectory = stageDirectory == null ? Path.of(".") : stageDirectory.getParent();
        return (projectDirectory == null ? Path.of(".") : projectDirectory).resolve(stage).resolve(fileName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DraftAgentResponse(
        @JsonProperty("section_count") int sectionCount
    ) {
    }
}
