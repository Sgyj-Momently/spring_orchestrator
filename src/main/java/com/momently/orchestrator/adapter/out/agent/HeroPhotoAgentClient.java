package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.HeroPhotoAgentProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * FastAPI 대표 사진 선택 에이전트를 HTTP로 호출하는 아웃바운드 어댑터다.
 *
 * <p>이 클래스는 {@link HeroPhotoAgentPort} 구현체로서, 애플리케이션 계층이 FastAPI의 URL,
 * JSON 필드·HTTP 클라이언트 세부에 의존하지 않게 한다. {@code stub-agents} 프로필이 아닐 때
 * 활성화되며, 로컬 개발용 대역 대신 실제 대표 사진 선택 서버를 호출한다.</p>
 */
@Component
@Profile("!stub-agents")
public class HeroPhotoAgentClient implements HeroPhotoAgentPort {

    private static final int ERROR_BODY_MAX_CHARS = 4_000;

    private final HeroPhotoAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HeroPhotoAgentClient(
        HeroPhotoAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public HeroPhotoResult selectHeroPhotos(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult
    ) {
        Path groupingResultPath = Path.of(photoGroupingResult.resultPath());
        Path resultPath = heroPhotoResultPath(groupingResultPath);
        Map<String, Object> payload = buildPayload(projectId, photoInfoResult, groupingResultPath);
        try {
            JsonNode responseBody = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

            if (responseBody == null || responseBody.isNull()) {
                throw new IllegalStateException("Hero photo agent returned an empty response");
            }

            writeHeroPhotoResult(resultPath, responseBody);
            HeroPhotoAgentResponse response = objectMapper.treeToValue(responseBody, HeroPhotoAgentResponse.class);
            return response.toResult(resultPath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist hero photo result: " + resultPath, exception);
        } catch (RestClientResponseException exception) {
            String responseBody = truncate(exception.getResponseBodyAsString(), ERROR_BODY_MAX_CHARS);
            throw new IllegalStateException(
                "Hero photo agent call failed. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", status="
                    + exception.getRawStatusCode()
                    + ", projectId="
                    + projectId
                    + ", groupingResultPath="
                    + photoGroupingResult.resultPath()
                    + ", responseBody="
                    + responseBody,
                exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                "Failed to call hero photo agent. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", projectId="
                    + projectId
                    + ", groupingResultPath="
                    + photoGroupingResult.resultPath()
                    + ", error="
                    + exception.getClass().getSimpleName()
                    + ": "
                    + exception.getMessage(),
                exception
            );
        }
    }

    private Map<String, Object> buildPayload(String projectId, PhotoInfoResult photoInfoResult, Path groupingResultPath) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("groups", readGroups(groupingResultPath));
        payload.put("photos", readPhotos(Path.of(photoInfoResult.bundlePath())));
        return payload;
    }

    private Object readGroups(Path groupingResultPath) {
        try {
            JsonNode root = objectMapper.readTree(groupingResultPath.toFile());
            JsonNode groups = root.path("groups");
            if (!groups.isArray()) {
                return java.util.List.of();
            }
            return objectMapper.convertValue(groups, Object.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read grouping result: " + groupingResultPath, exception);
        }
    }

    private List<Map<String, Object>> readPhotos(Path bundlePath) {
        try {
            JsonNode photos = objectMapper.readTree(bundlePath.toFile()).path("photos");
            List<Map<String, Object>> adaptedPhotos = new ArrayList<>();
            if (!photos.isArray()) {
                return adaptedPhotos;
            }
            for (JsonNode photo : photos) {
                if (photo.path("photo_summary").path("exclude_from_public_outputs").asBoolean(false)
                    || photo.path("exclude_from_public_outputs").asBoolean(false)) {
                    continue;
                }
                Map<String, Object> adaptedPhoto = adaptPhoto(photo);
                if (!adaptedPhoto.isEmpty()) {
                    adaptedPhotos.add(adaptedPhoto);
                }
            }
            return adaptedPhotos;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read photo info bundle: " + bundlePath, exception);
        }
    }

    private Map<String, Object> adaptPhoto(JsonNode photo) {
        JsonNode summary = photo.path("photo_summary");
        Map<String, Object> adapted = new LinkedHashMap<>();
        String fileName = textOrNull(photo.path("file_name"));
        if (fileName == null) {
            return adapted;
        }
        adapted.put("photo_id", "file:" + fileName);
        adapted.put("file_name", fileName);
        adapted.put("summary", textOrNull(summary.path("summary")));
        adapted.put("ocr_text", textListOrEmpty(summary.path("ocr_text")));
        adapted.put("confidence", numberOrNull(summary.path("confidence")));
        return adapted;
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

    private List<String> textListOrEmpty(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private Path heroPhotoResultPath(Path groupingResultPath) {
        Path groupingDirectory = groupingResultPath.getParent();
        Path outputDirectory = groupingDirectory == null ? Path.of(".") : groupingDirectory.getParent();
        if (outputDirectory == null) {
            outputDirectory = Path.of(".");
        }
        return outputDirectory.resolve("hero-photo").resolve("hero-result.json");
    }

    private void writeHeroPhotoResult(Path resultPath, JsonNode responseBody) throws IOException {
        Files.createDirectories(resultPath.getParent());
        JsonNode persisted = responseBody;
        if (responseBody instanceof ObjectNode objectNode) {
            persisted = objectNode.deepCopy();
            ((ObjectNode) persisted).put("artifact_type", "hero_photo_result");
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultPath.toFile(), persisted);
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
    record HeroPhotoAgentResponse(
        @JsonProperty("hero_photo_count") int heroPhotoCount
    ) {
        HeroPhotoResult toResult(String resultPath) {
            return new HeroPhotoResult(heroPhotoCount, resultPath);
        }
    }
}
