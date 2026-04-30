package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.StyleAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.momently.orchestrator.config.StyleAgentProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!stub-agents")
public class StyleAgentClient implements StyleAgentPort {

    private final StyleAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public StyleAgentClient(StyleAgentProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public StyleResult applyStyle(String projectId, DraftResult draftResult) {
        Path draftPath = Path.of(draftResult.resultPath());
        Path resultPath = siblingStagePath(draftPath, "style", "styled.json");
        JsonNode draft = readJson(draftPath);
        JsonNode body = post(Map.of(
            "project_id", projectId,
            "draft_markdown", draft.path("markdown").asText(""),
            "style", "warm_blog"
        ));
        writeResult(resultPath, body, "style_result");
        StyleAgentResponse response = convert(body, StyleAgentResponse.class);
        return new StyleResult(response.wordCount(), resultPath.toString());
    }

    private JsonNode post(Map<String, Object> payload) {
        try {
            JsonNode body = restClient.post().uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(payload).retrieve().body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw new IllegalStateException("Style agent returned an empty response");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Style agent call failed. status=%s, responseBody=%s"
                .formatted(exception.getRawStatusCode(), exception.getResponseBodyAsString()), exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to call style agent: " + exception.getMessage(), exception);
        }
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read json artifact: " + path, exception);
        }
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
            throw new IllegalStateException("Failed to persist style result: " + path, exception);
        }
    }

    private <T> T convert(JsonNode body, Class<T> type) {
        try {
            return objectMapper.treeToValue(body, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse style response", exception);
        }
    }

    private Path siblingStagePath(Path path, String stage, String fileName) {
        Path stageDirectory = path.getParent();
        Path projectDirectory = stageDirectory == null ? Path.of(".") : stageDirectory.getParent();
        return (projectDirectory == null ? Path.of(".") : projectDirectory).resolve(stage).resolve(fileName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StyleAgentResponse(@JsonProperty("word_count") int wordCount) {
    }
}

