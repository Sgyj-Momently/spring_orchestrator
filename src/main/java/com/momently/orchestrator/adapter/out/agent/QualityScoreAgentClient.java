package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.QualityScoreAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import com.momently.orchestrator.config.QualityScoreAgentProperties;
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
public class QualityScoreAgentClient implements QualityScoreAgentPort {

    private final QualityScoreAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public QualityScoreAgentClient(
        QualityScoreAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder builder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public QualityScoreResult scorePhotos(String projectId, PhotoInfoResult photoInfoResult) {
        Path inputBundlePath = Path.of(photoInfoResult.bundlePath());
        Path resultPath = siblingStagePath(inputBundlePath, "quality", "quality-result.json");
        Path scoredBundlePath = siblingStagePath(inputBundlePath, "quality", "bundle.json");
        JsonNode inputBundle = readJson(inputBundlePath);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("photos", arrayOrEmpty(inputBundle.path("photos")));

        JsonNode body = post(payload);
        writeResult(resultPath, body);
        writeScoredBundle(scoredBundlePath, inputBundle, body, resultPath);
        QualityScoreAgentResponse response = convert(body, QualityScoreAgentResponse.class);
        return new QualityScoreResult(
            response.photoCount(),
            response.averageScore(),
            resultPath.toString(),
            scoredBundlePath.toString()
        );
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
                throw new IllegalStateException("Quality score agent returned an empty response");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Quality score agent call failed. status=%s, responseBody=%s"
                .formatted(exception.getRawStatusCode(), exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call quality score agent: " + exception.getMessage(), exception);
        }
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read json artifact: " + path, exception);
        }
    }

    private Object arrayOrEmpty(JsonNode node) {
        return node.isArray() ? objectMapper.convertValue(node, Object.class) : java.util.List.of();
    }

    private void writeResult(Path path, JsonNode body) {
        try {
            Files.createDirectories(path.getParent());
            JsonNode persisted = body;
            if (body instanceof ObjectNode objectNode) {
                persisted = objectNode.deepCopy();
                ((ObjectNode) persisted).put("artifact_type", "quality_score_result");
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist quality score result: " + path, exception);
        }
    }

    private void writeScoredBundle(Path path, JsonNode inputBundle, JsonNode qualityResult, Path resultPath) {
        try {
            Files.createDirectories(path.getParent());
            ObjectNode scored = inputBundle.deepCopy();
            scored.set("photos", qualityResult.path("scored_photos"));
            scored.put("photo_count", qualityResult.path("photo_count").asInt(0));
            scored.put("quality_score_result_path", resultPath.toString());
            scored.put("average_quality_score", qualityResult.path("average_score").asDouble(0.0));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), scored);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist quality score bundle: " + path, exception);
        }
    }

    private <T> T convert(JsonNode body, Class<T> type) {
        try {
            return objectMapper.treeToValue(body, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse quality score response", exception);
        }
    }

    private Path siblingStagePath(Path path, String stage, String fileName) {
        Path stageDirectory = path.getParent();
        Path projectDirectory = stageDirectory == null ? Path.of(".") : stageDirectory.getParent();
        return (projectDirectory == null ? Path.of(".") : projectDirectory).resolve(stage).resolve(fileName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QualityScoreAgentResponse(
        @JsonProperty("photo_count") int photoCount,
        @JsonProperty("average_score") double averageScore
    ) {
    }
}

