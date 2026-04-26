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
        Files.createDirectories(inputRoot.resolve("project-001"));
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
        assertThat(result.blogPath()).isNull();
    }

    @Test
    @DisplayName("skipBlog이 false면 --skip-blog 옵션을 넣지 않는다")
    void omitsSkipBlogOptionWhenDisabled() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        List<String> capturedCommand = new ArrayList<>();
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, false),
            new ObjectMapper(),
            command -> {
                capturedCommand.addAll(command);
                writeBundleUnchecked(outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json"), 1);
                writeTextUnchecked(outputRoot.resolve("project-001").resolve("blog.md"), "# 블로그");
            }
        );

        PhotoInfoResult result = adapter.extractPhotoInfo("project-001");

        assertThat(capturedCommand).doesNotContain("--skip-blog");
        assertThat(result.blogPath()).isEqualTo(outputRoot.resolve("project-001").resolve("blog.md").toString());
    }

    @Test
    @DisplayName("파이프라인 후 bundle 파일이 없으면 실패로 처리한다")
    void failsWhenBundleFileMissing() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Photo info bundle not found");
    }

    @Test
    @DisplayName("입력 사진 폴더가 없으면 실패로 처리한다")
    void failsWhenInputDirectoryMissing() {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Photo input directory");
    }

    @Test
    @DisplayName("bundle JSON에 photo_count가 없으면 실패로 처리한다")
    void failsWhenBundleMissingPhotoCount() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        Path bundlePath = outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, "{\"photos\":[]}");
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("photo_count");
    }

    @Test
    @DisplayName("bundle JSON에 photos가 없으면 실패로 처리한다")
    void failsWhenBundleMissingPhotos() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        Path bundlePath = outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, "{\"photo_count\":0}");
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("photos");
    }

    @Test
    @DisplayName("bundle JSON의 photos가 배열이 아니면 실패로 처리한다")
    void failsWhenBundlePhotosIsNotArray() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        writeRawBundle(outputRoot, """
            {
              "photo_count": 0,
              "photos": {}
            }
            """);
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("photos must be an array");
    }

    @Test
    @DisplayName("bundle JSON의 photo_count와 photos 길이가 다르면 실패로 처리한다")
    void failsWhenBundlePhotoCountDoesNotMatchPhotosLength() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        writeRawBundle(outputRoot, """
            {
              "photo_count": 2,
              "photos": [ { "file_name": "a.jpg" } ]
            }
            """);
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not match photos.length");
    }

    @Test
    @DisplayName("bundle JSON의 photo_count가 정수가 아니면 실패로 처리한다")
    void failsWhenBundlePhotoCountIsNotInteger() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        writeRawBundle(outputRoot, """
            {
              "photo_count": "seven",
              "photos": []
            }
            """);
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be an integer");
    }

    @Test
    @DisplayName("bundle JSON의 photo_count가 음수면 실패로 처리한다")
    void failsWhenBundlePhotoCountIsNegative() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        writeRawBundle(outputRoot, """
            {
              "photo_count": -1,
              "photos": []
            }
            """);
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            properties(inputRoot, outputRoot, true),
            new ObjectMapper(),
            command -> {
            }
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be non-negative");
    }

    @Test
    @DisplayName("실제 프로세스 시작에 실패하면 애플리케이션 예외로 전파한다")
    void wrapsProcessStartFailure() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            new PhotoInfoPipelineProperties(
                tempDir.resolve("missing-python").toString(),
                "/workspace/run_pipeline.py",
                inputRoot.toString(),
                outputRoot.toString(),
                "http://ollama.test",
                "qwen2.5vl:7b",
                "gemma4",
                60,
                true,
                false
            ),
            new ObjectMapper()
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to start photo info pipeline");
    }

    @Test
    @DisplayName("실제 프로세스가 비정상 종료하면 로그 꼬리를 포함해 실패로 처리한다")
    void wrapsNonZeroProcessExitWithLogTail() throws IOException {
        Path inputRoot = tempDir.resolve("input");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(inputRoot.resolve("project-001"));
        Path scriptPath = tempDir.resolve("fail-pipeline.sh");
        Files.writeString(scriptPath, """
            echo pipeline failed loudly
            exit 7
            """);
        LocalPhotoInfoPipelineAdapter adapter = new LocalPhotoInfoPipelineAdapter(
            new PhotoInfoPipelineProperties(
                "/bin/sh",
                scriptPath.toString(),
                inputRoot.toString(),
                outputRoot.toString(),
                "http://ollama.test",
                "qwen2.5vl:7b",
                "gemma4",
                60,
                true,
                false
            ),
            new ObjectMapper()
        );

        assertThatThrownBy(() -> adapter.extractPhotoInfo("project-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("exit code 7")
            .hasMessageContaining("pipeline failed loudly");
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
            skipBlog,
            false
        );
    }

    private void writeBundle(Path bundlePath, int photoCount) throws IOException {
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, """
            {
              "photo_count": %d,
              "photos": %s
            }
            """.formatted(photoCount, dummyPhotosJson(photoCount)));
    }

    private void writeRawBundle(Path outputRoot, String content) throws IOException {
        Path bundlePath = outputRoot.resolve("project-001").resolve("bundles").resolve("bundle.json");
        Files.createDirectories(bundlePath.getParent());
        Files.writeString(bundlePath, content);
    }

    private String dummyPhotosJson(int count) {
        if (count <= 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("{\"file_name\":\"IMG_").append(index).append(".jpg\"}");
        }
        builder.append("]");
        return builder.toString();
    }

    private void writeBundleUnchecked(Path bundlePath, int photoCount) {
        try {
            writeBundle(bundlePath, photoCount);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void writeTextUnchecked(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
