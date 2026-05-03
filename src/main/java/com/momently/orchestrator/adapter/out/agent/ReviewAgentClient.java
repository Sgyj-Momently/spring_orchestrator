package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.momently.orchestrator.config.ReviewAgentProperties;
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
public class ReviewAgentClient implements ReviewAgentPort {

    private final ReviewAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ReviewAgentClient(ReviewAgentProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public ReviewResult reviewDocument(String projectId, PhotoInfoResult photoInfoResult, StyleResult styleResult) {
        Path stylePath = Path.of(styleResult.resultPath());
        Path resultPath = siblingStagePath(stylePath, "review", "final.json");
        JsonNode style = readJson(stylePath);
        JsonNode bundle = readJson(Path.of(photoInfoResult.bundlePath()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("styled_markdown", style.path("markdown").asText(""));
        payload.put("photos", arrayOrEmpty(bundle.path("photos")));
        payload.put("excluded_photos", arrayOrEmpty(bundle.path("excluded_photos")));

        JsonNode body = post(payload);
        requireReviewSemanticOk(body);
        writeResult(resultPath, body, "review_result");
        ReviewAgentResponse response = convert(body, ReviewAgentResponse.class);
        return new ReviewResult(response.issueCount(), resultPath.toString());
    }

    private static void requireReviewSemanticOk(JsonNode body) {
        JsonNode n = body.get("review_status");
        if (n == null || n.isNull() || n.asText("").isBlank()) {
            return;
        }
        String s = n.asText("").strip();
        if (!"ok".equals(s) && !"needs_attention".equals(s)) {
            throw new IllegalStateException("Review agent review_status is unexpected: " + s);
        }
    }

    private JsonNode post(Map<String, Object> payload) {
        try {
            JsonNode body = restClient.post().uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(payload).retrieve().body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw new IllegalStateException("Review agent returned an empty response");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Review agent call failed. status=%s, responseBody=%s"
                .formatted(exception.getRawStatusCode(), exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call review agent: " + exception.getMessage(), exception);
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
            throw new IllegalStateException("Failed to persist review result: " + path, exception);
        }
    }

    private <T> T convert(JsonNode body, Class<T> type) {
        try {
            return objectMapper.treeToValue(body, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse review response", exception);
        }
    }

    private Path siblingStagePath(Path path, String stage, String fileName) {
        Path stageDirectory = path.getParent();
        Path projectDirectory = stageDirectory == null ? Path.of(".") : stageDirectory.getParent();
        return (projectDirectory == null ? Path.of(".") : projectDirectory).resolve(stage).resolve(fileName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReviewAgentResponse(@JsonProperty("issue_count") int issueCount) {
    }
}

