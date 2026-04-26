package com.momently.orchestrator.adapter.out.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 현재 CLI 형태인 photo_exif_llm_pipeline을 로컬 프로세스로 실행하는 outbound adapter다.
 *
 * <p>사진 정보 추출 모듈은 아직 FastAPI 서버가 아니라 {@code run_pipeline.py} 명령으로 동작한다.
 * 이 adapter는 Spring 오케스트레이터가 그 CLI 실행 방식을 직접 품지 않도록 감싸며,
 * {@link PhotoInfoAgentPort} 계약에 맞춰 처리 사진 수와 bundle artifact 경로만 반환한다.</p>
 *
 * <p>{@code local-photo-info} 프로필에서만 활성화된다. 프로세스 시작 실패, 타임아웃, 비정상 종료,
 * bundle 경로 누락, bundle JSON 계약 위반, bundle 읽기 실패는 모두 {@link IllegalStateException}으로
 * 전파되어 워크플로 실패 기록의 원인이 된다.</p>
 */
@Component
@Profile("local-photo-info")
public class LocalPhotoInfoPipelineAdapter implements PhotoInfoAgentPort {

    private static final Path BUNDLE_FILE_RELATIVE = Path.of("bundles", "bundle.json");

    private final PhotoInfoPipelineProperties properties;
    private final ObjectMapper objectMapper;
    private final CommandExecutor commandExecutor;

    /**
     * 로컬 파이프라인 adapter를 생성한다.
     *
     * <p>운영 코드에서는 실제 {@link ProcessBuilder} 기반 executor를 사용한다. 테스트에서는 package-private
     * 생성자를 통해 executor를 주입해, Python 프로세스를 띄우지 않고 명령 구성과 결과 파일 읽기만 검증한다.</p>
     *
     * @param properties Python 실행 파일, 입력/출력 루트, 모델명 등 내부 실행 설정
     * @param objectMapper bundle JSON에서 {@code photo_count}를 읽기 위한 JSON mapper
     */
    public LocalPhotoInfoPipelineAdapter(
        PhotoInfoPipelineProperties properties,
        ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, new ProcessCommandExecutor(properties));
    }

    LocalPhotoInfoPipelineAdapter(
        PhotoInfoPipelineProperties properties,
        ObjectMapper objectMapper,
        CommandExecutor commandExecutor
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.commandExecutor = commandExecutor;
    }

    /**
     * 프로젝트 식별자에 해당하는 입력 폴더를 분석하고 bundle 결과를 반환한다.
     *
     * <p>입력 경로는 {@code inputRoot/projectId}, 출력 경로는 {@code outputRoot/projectId}로 계산한다.
     * CLI 실행이 성공하면 {@code outputRoot/projectId/bundles/bundle.json}을 읽어 사진 수와 artifact
     * 경로를 만든다. 경로와 모델 설정은 모두 내부 configuration에서 오며 공개 API 요청에는 포함되지 않는다.</p>
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @return 처리 사진 수와 생성된 bundle JSON 경로
     * @throws IllegalStateException 입력 경로가 없거나, 프로세스 실행·타임아웃·비정상 종료,
     *                                 bundle 누락, bundle 계약 위반, 읽기 실패 시 발생
     */
    @Override
    public PhotoInfoResult extractPhotoInfo(String projectId) {
        Path inputDir = Path.of(properties.inputRoot()).resolve(projectId);
        Path outputDir = Path.of(properties.outputRoot()).resolve(projectId);
        Path bundlePath = outputDir.resolve(BUNDLE_FILE_RELATIVE);

        if (!Files.isDirectory(inputDir)) {
            throw new IllegalStateException(
                "Photo input directory does not exist or is not a directory: " + inputDir
            );
        }

        commandExecutor.execute(buildCommand(inputDir, outputDir));

        if (!Files.isRegularFile(bundlePath)) {
            throw new IllegalStateException("Photo info bundle not found at expected path: " + bundlePath);
        }
        return readResult(bundlePath);
    }

    /**
     * {@code run_pipeline.py} CLI가 요구하는 인자 순서대로 명령 목록을 만든다.
     *
     * <p>{@link ProcessBuilder}에는 shell 문자열이 아니라 토큰 목록을 전달한다. 이렇게 해야 경로나 모델명에
     * 공백이 있어도 shell parsing 문제를 줄이고, 테스트에서 명령 계약을 정확히 비교할 수 있다.</p>
     *
     * @param inputDir 프로젝트별 사진 입력 디렉터리
     * @param outputDir 프로젝트별 파이프라인 산출물 디렉터리
     * @return Python 실행 파일부터 옵션까지 포함한 프로세스 명령 토큰
     */
    private List<String> buildCommand(Path inputDir, Path outputDir) {
        List<String> command = new ArrayList<>();
        command.add(properties.pythonExecutable());
        command.add(properties.scriptPath());
        command.add("--input");
        command.add(inputDir.toString());
        command.add("--output");
        command.add(outputDir.toString());
        command.add("--ollama-base-url");
        command.add(properties.ollamaBaseUrl());
        command.add("--vision-model");
        command.add(properties.visionModel());
        command.add("--writer-model");
        command.add(properties.writerModel());
        command.add("--ollama-timeout-seconds");
        command.add(String.valueOf(properties.ollamaTimeoutSeconds()));
        if (properties.skipBlog()) {
            command.add("--skip-blog");
        }
        return command;
    }

    /**
     * 파이프라인이 생성한 bundle JSON을 읽어 포트 결과로 축약한다.
     *
     * <p>현재 application 계층은 전체 bundle 본문을 직접 다루지 않는다. 큰 JSON은 artifact로 두고,
     * 오케스트레이션 판단에 필요한 {@code photo_count}와 경로만 반환한다.</p>
     *
     * @param bundlePath 파이프라인이 생성해야 하는 bundle JSON 경로
     * @return bundle의 사진 수와 artifact 경로
     */
    private PhotoInfoResult readResult(Path bundlePath) {
        try {
            JsonNode bundle = objectMapper.readTree(bundlePath.toFile());
            JsonNode countNode = bundle.get("photo_count");
            if (countNode == null || countNode.isNull()) {
                throw new IllegalStateException("bundle JSON missing required field photo_count: " + bundlePath);
            }
            if (!countNode.isNumber() || !countNode.isIntegralNumber()) {
                throw new IllegalStateException("bundle JSON field photo_count must be an integer: " + bundlePath);
            }
            int photoCount = countNode.intValue();
            if (photoCount < 0) {
                throw new IllegalStateException("bundle JSON field photo_count must be non-negative: " + bundlePath);
            }
            return new PhotoInfoResult(photoCount, bundlePath.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read photo info bundle: " + bundlePath, exception);
        }
    }

    /**
     * Ollama 단계 제한(초)에 비례한 JVM 기준의 파이프라인 전체 wall-clock 제한을 계산한다.
     *
     * <p>Python 쪽 {@code ollama-timeout-seconds}는 LLM 요청마다 쓰는 제한이고, 프로세스는 여러 장 처리와
     * I/O를 포함하므로 그보다 훨씬 긴 상한이 필요하다. 최소 5분, 최대 1시간으로 캡한다.</p>
     */
    private static Duration pipelineProcessTimeout(PhotoInfoPipelineProperties properties) {
        long seconds = Math.max(300L, Math.min(3_600L, (long) properties.ollamaTimeoutSeconds() * 30L));
        return Duration.ofSeconds(seconds);
    }

    /**
     * 로컬 프로세스 실행을 테스트에서 대체하기 위한 작은 경계다.
     */
    interface CommandExecutor {

        /**
         * 완성된 명령 토큰을 실행한다.
         *
         * @param command {@link ProcessBuilder}에 전달할 명령 토큰 목록
         */
        void execute(List<String> command);
    }

    /**
     * 실제 JVM 외부 프로세스로 Python 파이프라인을 실행하는 기본 executor다.
     */
    private static final class ProcessCommandExecutor implements CommandExecutor {

        private static final int LOG_TAIL_MAX_CHARS = 8_000;

        private final Duration processTimeout;

        private ProcessCommandExecutor(PhotoInfoPipelineProperties properties) {
            this.processTimeout = pipelineProcessTimeout(properties);
        }

        /**
         * Python 파이프라인 프로세스를 실행하고 종료 코드를 검증한다.
         *
         * <p>표준 출력과 표준 에러는 합쳐 임시 파일에 기록해 파이프가 막혀 Python이 멈추는 것을 막는다.
         * 비정상 종료·타임아웃일 때는 로그 끝부분을 예외 메시지에 붙인다. 인터럽트가 발생하면 스레드
         * 인터럽트 상태를 복구한 뒤 실패로 전파한다.</p>
         *
         * @param command 실행할 Python 파이프라인 명령 토큰
         * @throws IllegalStateException 프로세스를 시작할 수 없거나, 타임아웃이거나, 종료 코드가 0이 아니거나
         *     인터럽트된 경우
         */
        @Override
        public void execute(List<String> command) {
            Path logFile;
            try {
                logFile = Files.createTempFile("photo-info-pipeline", ".log");
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create temp file for photo info pipeline log", exception);
            }
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(logFile.toFile());
                try {
                    Process process = processBuilder.start();
                    boolean completed = process.waitFor(processTimeout.toSeconds(), TimeUnit.SECONDS);
                    if (!completed) {
                        process.destroyForcibly();
                        String tail = readLogTail(logFile, LOG_TAIL_MAX_CHARS);
                        throw new IllegalStateException(
                            "Photo info pipeline timed out after "
                                + processTimeout.toSeconds()
                                + "s. Log tail: "
                                + tail
                        );
                    }
                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        String tail = readLogTail(logFile, LOG_TAIL_MAX_CHARS);
                        throw new IllegalStateException(
                            "Photo info pipeline failed with exit code " + exitCode + ". Log tail: " + tail
                        );
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to start photo info pipeline", exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Photo info pipeline was interrupted", exception);
                }
            } finally {
                try {
                    Files.deleteIfExists(logFile);
                } catch (IOException ignored) {
                }
            }
        }

        private static String readLogTail(Path logFile, int maxChars) {
            if (!Files.isRegularFile(logFile)) {
                return "";
            }
            try {
                String text = Files.readString(logFile, StandardCharsets.UTF_8);
                if (text.length() <= maxChars) {
                    return text;
                }
                return text.substring(text.length() - maxChars);
            } catch (IOException exception) {
                return "(could not read pipeline log: " + exception.getMessage() + ")";
            }
        }
    }
}
