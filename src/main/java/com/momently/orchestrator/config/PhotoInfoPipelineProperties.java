package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로컬 사진 정보 추출 CLI 파이프라인 실행에 필요한 내부 설정이다.
 *
 * <p>사진 정보 추출 모듈은 아직 HTTP 서비스가 아니라 {@code run_pipeline.py} CLI로 제공된다.
 * 따라서 Spring 오케스트레이터는 이 설정을 통해 실행 파일, 입력/출력 루트, 모델 값을 내부적으로
 * 조립한다. 이 값들은 공개 API 계약에 노출하지 않고 운영 설정으로만 변경한다.</p>
 *
 * <p>Spring Boot 3.5에서 {@code @ConfigurationProperties} record의 컴팩트 생성자 조합 시
 * 빈 팩토리가 무인자 생성자를 찾다 실패하는 경우가 있어, 생성자 바인딩에 안전한 불변 클래스로 둔다.</p>
 */
@ConfigurationProperties(prefix = "agents.photo-info.pipeline")
public final class PhotoInfoPipelineProperties {

    private final String pythonExecutable;
    private final String scriptPath;
    private final String inputRoot;
    private final String outputRoot;
    private final String ollamaBaseUrl;
    private final String visionModel;
    private final String writerModel;
    private final int ollamaTimeoutSeconds;
    private final String ffmpegCommand;
    private final double videoFrameSecond;
    private final int videoFrameCount;
    private final double videoFrameIntervalSeconds;
    private final int analysisConcurrency;
    private final boolean skipBlog;
    private final boolean force;

    public PhotoInfoPipelineProperties(
        String pythonExecutable,
        String scriptPath,
        String inputRoot,
        String outputRoot,
        String ollamaBaseUrl,
        String visionModel,
        String writerModel,
        int ollamaTimeoutSeconds,
        String ffmpegCommand,
        double videoFrameSecond,
        int videoFrameCount,
        double videoFrameIntervalSeconds,
        int analysisConcurrency,
        boolean skipBlog,
        boolean force
    ) {
        this.pythonExecutable = defaultIfBlank(pythonExecutable, "python3");
        this.scriptPath = defaultIfBlank(scriptPath, "../photo_exif_llm_pipeline/src/run_pipeline.py");
        this.inputRoot = defaultIfBlank(inputRoot, "../photo_exif_llm_pipeline/input_photos");
        this.outputRoot = defaultIfBlank(outputRoot, "../photo_exif_llm_pipeline/output/orchestrator");
        this.ollamaBaseUrl = defaultIfBlank(ollamaBaseUrl, "http://localhost:11434");
        this.visionModel = defaultIfBlank(visionModel, "qwen2.5vl:7b");
        this.writerModel = defaultIfBlank(writerModel, "qwen2.5:14b");
        int timeout = ollamaTimeoutSeconds;
        if (timeout <= 0) {
            timeout = 180;
        }
        this.ollamaTimeoutSeconds = timeout;
        this.ffmpegCommand = defaultIfBlank(ffmpegCommand, "ffmpeg");
        double vfs = videoFrameSecond;
        if (vfs <= 0.0) {
            vfs = 1.0;
        }
        this.videoFrameSecond = vfs;
        int vfc = videoFrameCount;
        if (vfc <= 0) {
            vfc = 3;
        }
        this.videoFrameCount = vfc;
        double vfi = videoFrameIntervalSeconds;
        if (vfi <= 0.0) {
            vfi = 4.0;
        }
        this.videoFrameIntervalSeconds = vfi;
        int concurrency = analysisConcurrency;
        if (concurrency <= 0) {
            concurrency = 4;
        }
        this.analysisConcurrency = Math.min(concurrency, 16);
        this.skipBlog = skipBlog;
        this.force = force;
    }

    public String pythonExecutable() {
        return pythonExecutable;
    }

    public String scriptPath() {
        return scriptPath;
    }

    public String inputRoot() {
        return inputRoot;
    }

    public String outputRoot() {
        return outputRoot;
    }

    public String ollamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public String visionModel() {
        return visionModel;
    }

    public String writerModel() {
        return writerModel;
    }

    public int ollamaTimeoutSeconds() {
        return ollamaTimeoutSeconds;
    }

    public String ffmpegCommand() {
        return ffmpegCommand;
    }

    public double videoFrameSecond() {
        return videoFrameSecond;
    }

    public int videoFrameCount() {
        return videoFrameCount;
    }

    public double videoFrameIntervalSeconds() {
        return videoFrameIntervalSeconds;
    }

    public int analysisConcurrency() {
        return analysisConcurrency;
    }

    public boolean skipBlog() {
        return skipBlog;
    }

    public boolean force() {
        return force;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
