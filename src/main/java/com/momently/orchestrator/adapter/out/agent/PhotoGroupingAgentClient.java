package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.PhotoGroupingAgentProperties;
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
 * FastAPI 사진 그룹화 에이전트를 HTTP로 호출하는 outbound adapter다.
 *
 * <p>이 adapter는 {@link PhotoGroupingAgentPort} 구현체로서, application 계층이 FastAPI의 URL,
 * JSON field naming, HTTP client 세부사항에 의존하지 않게 한다. {@code stub-agents} 프로필이 아닐 때
 * 활성화되며, 로컬 개발 stub 대신 실제 그룹화 서버를 호출한다.</p>
 *
 * <p>응답 JSON 전체를 application 계층으로 흘리지 않고, 현재 오케스트레이션에 필요한
 * {@code grouping_strategy}, {@code group_count}만 {@link PhotoGroupingResult}로 변환한다.
 * HTTP 오류나 네트워크 오류는 {@link IllegalStateException}으로 감싸 워크플로 실패 기록으로 이어지게 한다.</p>
 */
@Component
@Profile("!stub-agents")
public class PhotoGroupingAgentClient implements PhotoGroupingAgentPort {

    private static final int ERROR_BODY_MAX_CHARS = 4_000;

    private final PhotoGroupingAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * HTTP client와 에이전트 설정을 생성한다.
     *
     * <p>{@code baseUrl}은 {@link PhotoGroupingAgentProperties}에서만 가져온다. 공개 API 요청에서
     * 에이전트 주소를 받지 않으므로, 클라이언트가 내부 인프라 위치를 바꾸는 일을 막을 수 있다.</p>
     *
     * @param properties 그룹화 에이전트 base URL과 endpoint 경로 설정
     * @param objectMapper 사진 정보 bundle artifact를 HTTP 요청 DTO로 변환하기 위한 JSON mapper
     * @param restClientBuilder Spring이 제공하는 HTTP client builder
     */
    public PhotoGroupingAgentClient(
        PhotoGroupingAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * 사진 정보 bundle을 FastAPI 에이전트 요청으로 변환해 POST하고 요약 결과로 변환한다.
     *
     * <p>사진 정보 추출 adapter가 남긴 {@code bundle.json}에서 {@code photos} 배열을 읽고,
     * 그룹화 에이전트의 {@code /api/v1/photo-groups} 계약에 맞춰 {@code project_id},
     * {@code grouping_strategy}, {@code photos}를 포함한 요청을 만든다. FastAPI 응답에
     * {@code groups} 같은 큰 본문이 포함되어도 이 단계에서는 상태 전이에 필요한 요약만 읽는다.</p>
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @param groupingStrategy 워크플로 생성 시 선택된 그룹화 전략
     * @param photoInfoResult 사진 정보 추출 단계가 남긴 bundle artifact 참조
     * @return 실제 적용 전략과 생성 그룹 수를 담은 결과
     * @throws IllegalStateException bundle 읽기 실패, 빈 응답, HTTP 오류, 네트워크 오류가 발생한 경우
     */
    @Override
    public PhotoGroupingResult groupPhotos(
        String projectId,
        String groupingStrategy,
        int timeWindowMinutes,
        PhotoInfoResult photoInfoResult
    ) {
        Map<String, Object> payload = buildPayload(projectId, groupingStrategy, timeWindowMinutes, photoInfoResult);
        Path resultPath = groupingResultPath(Path.of(photoInfoResult.bundlePath()));
        try {
            JsonNode responseBody = restClient.post()
                .uri(properties.endpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

            if (responseBody == null || responseBody.isNull()) {
                throw new IllegalStateException("Photo grouping agent returned an empty response");
            }
            writeGroupingResult(resultPath, responseBody);
            PhotoGroupingAgentResponse response = objectMapper.treeToValue(responseBody, PhotoGroupingAgentResponse.class);
            return response.toResult(resultPath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist photo grouping result: " + resultPath, exception);
        } catch (RestClientResponseException exception) {
            int photoCount = payloadPhotoCount(payload);
            String responseBody = truncate(exception.getResponseBodyAsString(), ERROR_BODY_MAX_CHARS);
            throw new IllegalStateException(
                "Photo grouping agent call failed. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", status="
                    + exception.getRawStatusCode()
                    + ", projectId="
                    + projectId
                    + ", groupingStrategy="
                    + groupingStrategy
                    + ", photoCount="
                    + photoCount
                    + ", responseBody="
                    + responseBody,
                exception
            );
        } catch (RestClientException exception) {
            int photoCount = payloadPhotoCount(payload);
            throw new IllegalStateException(
                "Failed to call photo grouping agent. "
                    + "endpoint="
                    + properties.baseUrl()
                    + properties.endpoint()
                    + ", projectId="
                    + projectId
                    + ", groupingStrategy="
                    + groupingStrategy
                    + ", photoCount="
                    + photoCount
                    + ", error="
                    + exception.getClass().getSimpleName()
                    + ": "
                    + exception.getMessage(),
                exception
            );
        }
    }

    private static int payloadPhotoCount(Map<String, Object> payload) {
        Object photos = payload.get("photos");
        if (photos instanceof List<?> list) {
            return list.size();
        }
        return -1;
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

    private Map<String, Object> buildPayload(
        String projectId,
        String groupingStrategy,
        int timeWindowMinutes,
        PhotoInfoResult photoInfoResult
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", projectId);
        payload.put("grouping_strategy", groupingStrategy);
        payload.put("time_window_minutes", timeWindowMinutes);
        payload.put("photos", readPhotos(Path.of(photoInfoResult.bundlePath())));
        return payload;
    }

    private Path groupingResultPath(Path bundlePath) {
        Path bundleDirectory = bundlePath.getParent();
        Path outputDirectory = bundleDirectory == null ? Path.of(".") : bundleDirectory.getParent();
        if (outputDirectory == null) {
            outputDirectory = Path.of(".");
        }
        return outputDirectory.resolve("grouping").resolve("grouping-result.json");
    }

    private void writeGroupingResult(Path resultPath, JsonNode responseBody) throws IOException {
        Files.createDirectories(resultPath.getParent());
        JsonNode persisted = responseBody;
        if (responseBody instanceof ObjectNode objectNode) {
            persisted = objectNode.deepCopy();
            ((ObjectNode) persisted).put("artifact_type", "photo_grouping_result");
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultPath.toFile(), persisted);
    }

    private List<Map<String, Object>> readPhotos(Path bundlePath) {
        try {
            JsonNode photos = objectMapper.readTree(bundlePath.toFile()).path("photos");
            List<Map<String, Object>> adaptedPhotos = new ArrayList<>();
            if (!photos.isArray()) {
                return adaptedPhotos;
            }
            int index = 1;
            for (JsonNode photo : photos) {
                if (photo.path("photo_summary").path("exclude_from_public_outputs").asBoolean(false)
                    || photo.path("exclude_from_public_outputs").asBoolean(false)) {
                    continue;
                }
                adaptedPhotos.add(adaptPhoto(index, photo));
                index++;
            }
            return adaptedPhotos;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read photo info bundle: " + bundlePath, exception);
        }
    }

    private Map<String, Object> adaptPhoto(int index, JsonNode photo) {
        JsonNode summary = photo.path("photo_summary");
        Map<String, Object> adaptedPhoto = new LinkedHashMap<>();
        String fileName = textOrNull(photo.path("file_name"));
        if (fileName == null) {
            fileName = "photo-%03d".formatted(index);
        }
        adaptedPhoto.put("photo_id", stablePhotoId(fileName));
        adaptedPhoto.put("file_name", fileName);
        adaptedPhoto.put("captured_at", textOrNull(photo.path("captured_at")));
        adaptedPhoto.put("has_gps", booleanOrNull(photo.path("has_gps")));
        adaptedPhoto.put("gps", nullOrNode(photo.path("gps")));
        adaptedPhoto.put("location_hint", textOrNull(summary.path("location_hint")));
        adaptedPhoto.put("scene_type", textOrNull(summary.path("scene_type")));
        adaptedPhoto.put("summary", textOrNull(summary.path("summary")));
        adaptedPhoto.put("subjects", textListOrEmpty(summary.path("subjects")));
        return adaptedPhoto;
    }

    private static String stablePhotoId(String fileName) {
        return "file:" + fileName;
    }

    private String textOrFallback(JsonNode node, String fallback) {
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return fallback;
        }
        return node.asText();
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private Boolean booleanOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asBoolean();
    }

    private Object nullOrNode(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node;
    }

    private List<String> textListOrEmpty(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    /**
     * 그룹화 에이전트 응답 중 오케스트레이터가 즉시 필요로 하는 필드다.
     *
     * <p>FastAPI 응답에는 그룹 목록과 LLM 보정 메타데이터가 더 들어올 수 있다. 이 record는 그 값을
     * 무시하고 application port 결과에 필요한 필드만 안정적으로 역직렬화한다.</p>
     *
     * @param groupingStrategy FastAPI가 실제 적용했다고 응답한 그룹화 전략
     * @param groupCount FastAPI가 생성했다고 응답한 그룹 수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotoGroupingAgentResponse(
        @JsonProperty("grouping_strategy") String groupingStrategy,
        @JsonProperty("group_count") int groupCount
    ) {

        /**
         * HTTP 응답 DTO를 application port 결과로 변환한다.
         *
         * @return application 계층이 사용하는 그룹화 요약 결과
         */
        PhotoGroupingResult toResult(String resultPath) {
            return new PhotoGroupingResult(groupingStrategy, groupCount, resultPath);
        }
    }
}
