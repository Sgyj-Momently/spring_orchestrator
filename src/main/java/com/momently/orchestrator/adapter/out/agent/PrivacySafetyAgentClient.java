package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.PrivacySafetyAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import com.momently.orchestrator.config.PrivacySafetyAgentProperties;
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
public class PrivacySafetyAgentClient implements PrivacySafetyAgentPort {

    private final PrivacySafetyAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public PrivacySafetyAgentClient(
        PrivacySafetyAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder builder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public PrivacySafetyResult reviewPrivacy(String projectId, PhotoInfoResult photoInfoResult) {
        Path originalBundlePath = Path.of(photoInfoResult.bundlePath());
        Path resultPath = siblingStagePath(originalBundlePath, "privacy", "privacy-result.json");
        Path sanitizedBundlePath = siblingStagePath(originalBundlePath, "privacy", "bundle.json");
        JsonNode originalBundle = readJson(originalBundlePath);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("photos", arrayOrEmpty(originalBundle.path("photos")));

        JsonNode body = post(payload);
        writeResult(resultPath, body);
        writeSanitizedBundle(sanitizedBundlePath, originalBundle, body);
        PrivacySafetyAgentResponse response = convert(body, PrivacySafetyAgentResponse.class);
        return new PrivacySafetyResult(
            response.publicPhotoCount(),
            response.excludedPhotoCount(),
            resultPath.toString(),
            sanitizedBundlePath.toString()
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
                throw new IllegalStateException("Privacy safety agent returned an empty response");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Privacy safety agent call failed. status=%s, responseBody=%s"
                .formatted(exception.getRawStatusCode(), exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call privacy safety agent: " + exception.getMessage(), exception);
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
                ((ObjectNode) persisted).put("artifact_type", "privacy_safety_result");
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist privacy safety result: " + path, exception);
        }
    }

    private void writeSanitizedBundle(Path path, JsonNode originalBundle, JsonNode privacyResult) {
        try {
            Files.createDirectories(path.getParent());
            ObjectNode sanitized = originalBundle.deepCopy();
            sanitized.set("photos", privacyResult.path("public_photos"));
            sanitized.set("excluded_photos", privacyResult.path("excluded_photos"));
            sanitized.put("photo_count", privacyResult.path("public_photo_count").asInt(0));
            sanitized.put("privacy_result_path", siblingStagePath(Path.of(path.toString()), "privacy", "privacy-result.json").toString());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), sanitized);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist sanitized privacy bundle: " + path, exception);
        }
    }

    private <T> T convert(JsonNode body, Class<T> type) {
        try {
            return objectMapper.treeToValue(body, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse privacy safety response", exception);
        }
    }

    private Path siblingStagePath(Path path, String stage, String fileName) {
        Path stageDirectory = path.getParent();
        Path projectDirectory = stageDirectory == null ? Path.of(".") : stageDirectory.getParent();
        return (projectDirectory == null ? Path.of(".") : projectDirectory).resolve(stage).resolve(fileName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PrivacySafetyAgentResponse(
        @JsonProperty("public_photo_count") int publicPhotoCount,
        @JsonProperty("excluded_photo_count") int excludedPhotoCount
    ) {
    }
}

