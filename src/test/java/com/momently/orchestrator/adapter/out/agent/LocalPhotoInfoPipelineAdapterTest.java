package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 로컬 사진 정보 파이프라인 adapter의 명령 구성과 결과 읽기를 검증한다.
 */
class LocalPhotoInfoPipelineAdapterTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("프로젝트별 입력/출력 경로로 CLI를 실행하고 bundle 결과를 반환한다")
    void runsPipelineAndReturnsBundleResult() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        List<String> capturedCommand = new ArrayList<>();
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
                capturedCommand.addAll(command);
                writeBundleUnchecked(outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json"), 7);
            }
        );

        PhotoInfoResult result = adapter.extractPhotoInfo("project-001");

        assertThat(capturedCommand).containsExactly(
            "python",
            "/workspace/run_pipeline.py",
            "--input",
            inputRoot.resolve("project-001").toString(),
            "--output",
            outputRoot.resolve("project-001").toString(),
            "--ollama-base-url",
            "http://ollama.test",
            "--vision-model",
            "qwen2.5vl:7b",
            "--writer-model",
            "gemma4",
            "--ollama-timeout-seconds",
            "60",
            "--skip-blog"
        );
        assertThat(result.photoCount()).isEqualTo(7);
        assertThat(result.bundlePath()).isEqualTo(
            outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json").toString()
        );
    }

    @Test
    @DisplayName("skipBlog이 false면 --skip-blog 옵션을 넣지 않는다")
    void omitsSkipBlogOptionWhenDisabled() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        List<String> capturedCommand = new ArrayList<>();
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, false),
            new ObjectMapper(),
            command -> {
                capturedCommand.addAll(command);
                writeBundleUnchecked(outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json"), 1);
            }
        );

        adapter.extractPhotoInfo("project-001");

        assertThat(capturedCommand).doesNotContain("--skip-blog");
    }

    @Test
    @DisplayName("bundle 결과 파일을 읽을 수 없으면 실패로 처리한다")
    void failsWhenBundleCannotBeRead() {
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(tempDir.resolve("input"), tempDir.resolve("output"), true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read photo info bundle");
    }

    private PhotoInfoPipelineProperties properties(Path inputRoot, Path outputRoot, boolean skipBlog) {
        return new PhotoInfoPipelineProperties(
            "python",
            "/workspace/run_pipeline.py",
            inputRoot.toString(),
            outputRoot.toString(),
            "http://ollama.test",
            "qwen2.5vl:7b",
            "gemma4",
            60,
            skipBlog
        );
    }

    private void writeBundle(Path bundlePath, int photoCount) throws IOException {
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, """
            {
              "photo_count": %d,
              "photos": []
            }
            """.formatted(photoCount));
    }

    private void writeBundleUnchecked(Path bundlePath, int photoCount) {
        try {
            writeBundle(bundlePath, photoCount);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
