package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.OutlineAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.OutlineAgentProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!stub-agents")
public class OutlineAgentClient implements OutlineAgentPort {

    private static final int ERROR_BODY_MAX_CHARS = 4_000;

    private final OutlineAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OutlineAgentClient(
        OutlineAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public OutlineResult createOutline(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult
    ) {
        Path groupingResultPath = Path.of(photoGroupingResult.resultPath());
        Path heroResultPath = Path.of(heroPhotoResult.resultPath());
        Path resultPath = outlineResultPath(groupingResultPath);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("groups", readJsonArray(groupingResultPath, "groups"));
        payload.put("hero_photos", readJsonArray(heroResultPath, "hero_photos"));
        payload.put("photos", readOutlinePhotos(Path.of(photoInfoResult.bundlePath())));

        try {
            JsonNode responseBody = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

            if (responseBody == null || responseBody.isNull()) {
                throw new IllegalStateException("Outline agent returned an empty response");
            }
            writeOutlineResult(resultPath, responseBody);
            OutlineAgentResponse response = objectMapper.treeToValue(responseBody, OutlineAgentResponse.class);
            int sectionCount = response.sectionCount();
            return new OutlineResult(sectionCount, resultPath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist outline result: " + resultPath, exception);
        } catch (RestClientResponseException exception) {
            String responseBody = truncate(exception.getResponseBodyAsString(), ERROR_BODY_MAX_CHARS);
            throw new IllegalStateException(
                "Outline agent call failed. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", status="
                    + exception.getRawStatusCode()
                    + ", projectId="
                    + projectId
                    + ", responseBody="
                    + responseBody,
                exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                "Failed to call outline agent. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", projectId="
                    + projectId
                    + ", error="
                    + exception.getClass().getSimpleName()
                    + ": "
                    + exception.getMessage(),
                exception
            );
        }
    }

    private Object readJsonArray(Path jsonPath, String field) {
        try {
            JsonNode root = objectMapper.readTree(jsonPath.toFile());
            JsonNode node = root.path(field);
            if (!node.isArray()) {
                return java.util.List.of();
            }
            return objectMapper.convertValue(node, Object.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read json artifact: " + jsonPath, exception);
        }
    }

    private Object readOutlinePhotos(Path bundlePath) {
        try {
            JsonNode photos = objectMapper.readTree(bundlePath.toFile()).path("photos");
            if (!photos.isArray()) {
                return java.util.List.of();
            }
            // 그대로 넘기되, 사진 요약만 쓰는 outline_agent가 소비할 필드만 유지하도록 최소화
            java.util.List<Map<String, Object>> adapted = new java.util.ArrayList<>();
            for (JsonNode photo : photos) {
                if (photo.path("photo_summary").path("exclude_from_public_outputs").asBoolean(false)
                    || photo.path("exclude_from_public_outputs").asBoolean(false)) {
                    continue;
                }
                String fileName = textOrNull(photo.path("file_name"));
                if (fileName == null) {
                    continue;
                }
                JsonNode summary = photo.path("photo_summary");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("photo_id", "file:" + fileName);
                item.put("file_name", fileName);
                item.put("summary", textOrNull(summary.path("summary")));
                item.put("ocr_text", textListOrEmpty(summary.path("ocr_text")));
                item.put("confidence", numberOrNull(summary.path("confidence")));
                adapted.add(item);
            }
            return adapted;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read photo info bundle: " + bundlePath, exception);
        }
    }

    private Path outlineResultPath(Path groupingResultPath) {
        Path groupingDirectory = groupingResultPath.getParent();
        Path outputDirectory = groupingDirectory == null ? Path.of(".") : groupingDirectory.getParent();
        if (outputDirectory == null) {
            outputDirectory = Path.of(".");
        }
        return outputDirectory.resolve("outline").resolve("outline.json");
    }

    private void writeOutlineResult(Path resultPath, JsonNode responseBody) throws IOException {
        Files.createDirectories(resultPath.getParent());
        JsonNode persisted = responseBody;
        if (responseBody instanceof ObjectNode objectNode) {
            persisted = objectNode.deepCopy();
            ((ObjectNode) persisted).put("artifact_type", "outline_result");
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultPath.toFile(), persisted);
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private Double numberOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            return null;
        }
        return node.doubleValue();
    }

    private java.util.List<String> textListOrEmpty(JsonNode node) {
        if (!node.isArray()) {
            return java.util.List.of();
        }
        java.util.List<String> values = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String truncate(String value, int maxChars) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...(truncated)";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OutlineAgentResponse(
        @JsonProperty("outline_status") String outlineStatus,
        @JsonProperty("outline") JsonNode outline
    ) {
        int sectionCount() {
            if (outline == null || outline.isNull()) {
                return 0;
            }
            JsonNode sections = outline.path("sections");
            return sections.isArray() ? sections.size() : 0;
        }
    }
}

