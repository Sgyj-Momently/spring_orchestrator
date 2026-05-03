package com.momently.orchestrator.adapter.in.web.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.config.MomentlyUploadProperties;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 업로드 보안 검증과 저장 결과를 검증한다.
 */
class PhotoUploadServiceTest {

    @TempDir
    Path uploadRoot;

    @Test
    @DisplayName("유효한 PNG를 새 프로젝트 폴더에 저장하고 응답을 반환한다")
    void savesPngIntoNewProjectDirectory() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-save"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        byte[] png = tinyPng();
        MockMultipartFile part = new MockMultipartFile(
            "files",
            "shot.png",
            "image/png",
            png
        );

        PhotoUploadResponse response = service.saveUploadedImages(java.util.List.of(part));

        assertThat(response.savedCount()).isEqualTo(1);
        Path projectDir = inputRoot.resolve(response.projectId());
        assertThat(projectDir.resolve("0001.png")).exists();
        assertThat(response.bytesTotal()).isEqualTo(png.length);
        assertThat(response.projectId()).startsWith("u_");
    }

    @Test
    @DisplayName("PNG 시그니처만 있고 본문이 깨진 파일은 디코딩 단계에서 거절한다")
    void rejectsTruncatedPayloadEvenWhenPngSignatureMatches() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-fakepng"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        byte[] pngSignatureTruncatedRest = new byte[] {
            (byte) 0x89,
            0x50,
            0x4e,
            0x47,
            0x0d,
            0x0a,
            0x1a,
            0x0a,
            0x00,
            0x01,
            0x02,
        };

        MockMultipartFile part = new MockMultipartFile(
            "files",
            "spoof.png",
            "image/png",
            pngSignatureTruncatedRest
        );

        assertThatThrownBy(() -> service.saveUploadedImages(java.util.List.of(part)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미지 내용 검증 실패")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("JPEG도 저장·검증을 통과한다")
    void savesJpegIntoNewProjectDirectory() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-jpeg"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", buffer);
        byte[] jpegBytes = buffer.toByteArray();

        MockMultipartFile part = new MockMultipartFile(
            "files",
            "snap.jpg",
            "image/jpeg",
            jpegBytes
        );

        PhotoUploadResponse response = service.saveUploadedImages(java.util.List.of(part));

        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.projectId()).startsWith("u_");
        assertThat(inputRoot.resolve(response.projectId()).resolve("0001.jpg")).exists();
    }

    @Test
    @DisplayName("실제 바이트가 PNG인데 확장자만 JPG이면 헤더 검증에서 거절한다")
    void rejectsMismatchedExtensionAndContent() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in2"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        byte[] png = tinyPng();
        MockMultipartFile part = new MockMultipartFile(
            "files",
            "fake.jpg",
            "image/jpeg",
            png
        );

        assertThatThrownBy(() -> service.saveUploadedImages(java.util.List.of(part)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("형식과 맞지 않");
    }

    private PhotoUploadService newPhotoUploadService(String inputRoot) {
        PhotoInfoPipelineProperties pipeline = new PhotoInfoPipelineProperties(
            "python3",
            "../photo_exif_llm_pipeline/src/run_pipeline.py",
            inputRoot,
            uploadRoot.resolve("out").toString(),
            "http://localhost:11434",
            "dummy",
            "dummy",
            60,
            true,
            false
        );
        MomentlyUploadProperties limits = new MomentlyUploadProperties(10, 2_000_000, 12_000_000);
        return new PhotoUploadService(pipeline, limits);
    }

    private static byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }
}
