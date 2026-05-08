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
    @DisplayName("유효한 MP4 헤더를 가진 동영상도 새 프로젝트 폴더에 저장한다")
    void savesMp4IntoNewProjectDirectory() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-mp4"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        byte[] mp4 = fakeIsoBmff("isom");
        MockMultipartFile part = new MockMultipartFile(
            "files",
            "clip.mp4",
            "video/mp4",
            mp4
        );

        PhotoUploadResponse response = service.saveUploadedMedia(java.util.List.of(part));

        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.projectId()).startsWith("u_");
        assertThat(inputRoot.resolve(response.projectId()).resolve("0001.mp4")).exists();
        assertThat(response.bytesTotal()).isEqualTo(mp4.length);
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

    @Test
    @DisplayName("빈 업로드 요청은 거절한다")
    void rejectsEmptyUpload() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-empty"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());

        assertThatThrownBy(() -> service.saveUploadedMedia(java.util.List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("업로드할 사진 또는 동영상 파일이 없습니다");
    }

    @Test
    @DisplayName("파일 개수 제한을 넘으면 저장 전에 거절한다")
    void rejectsTooManyFiles() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-too-many"));
        PhotoUploadService service = newPhotoUploadService(
            inputRoot.toString(),
            new MomentlyUploadProperties(1, 2_000_000, 12_000_000)
        );

        MockMultipartFile a = new MockMultipartFile("files", "a.mp4", "video/mp4", fakeIsoBmff("isom"));
        MockMultipartFile b = new MockMultipartFile("files", "b.mp4", "video/mp4", fakeIsoBmff("isom"));

        assertThatThrownBy(() -> service.saveUploadedMedia(java.util.List.of(a, b)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 1개");
    }

    @Test
    @DisplayName("전체 용량 제한을 넘으면 저장 전에 거절한다")
    void rejectsDeclaredTotalOverLimit() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-total-limit"));
        PhotoUploadService service = newPhotoUploadService(
            inputRoot.toString(),
            new MomentlyUploadProperties(10, 2_000_000, 10)
        );
        MockMultipartFile part = new MockMultipartFile("files", "clip.mp4", "video/mp4", fakeIsoBmff("isom"));

        assertThatThrownBy(() -> service.saveUploadedMedia(java.util.List.of(part)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("전체 업로드 용량 한도");
    }

    @Test
    @DisplayName("입력 루트가 없으면 서비스 오류로 거절한다")
    void rejectsMissingInputRoot() {
        PhotoUploadService service = newPhotoUploadService(uploadRoot.resolve("missing-root").toString());
        MockMultipartFile part = new MockMultipartFile("files", "clip.mp4", "video/mp4", fakeIsoBmff("isom"));

        assertThatThrownBy(() -> service.saveUploadedMedia(java.util.List.of(part)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("미디어 입력 루트가 존재하지 않습니다");
    }

    @Test
    @DisplayName("확장자가 없어도 동영상 MIME으로 저장 확장자를 결정한다")
    void usesVideoMimeTypeWhenExtensionIsMissing() throws Exception {
        Path inputRoot = Files.createDirectories(uploadRoot.resolve("in-video-mime"));
        PhotoUploadService service = newPhotoUploadService(inputRoot.toString());
        MockMultipartFile mov = new MockMultipartFile("files", "clip", "video/quicktime", fakeIsoBmff("qt  "));
        MockMultipartFile m4v = new MockMultipartFile("files", "clip2", "video/x-m4v", fakeIsoBmff("M4V "));

        PhotoUploadResponse response = service.saveUploadedMedia(java.util.List.of(mov, m4v));

        assertThat(inputRoot.resolve(response.projectId()).resolve("0001.mov")).exists();
        assertThat(inputRoot.resolve(response.projectId()).resolve("0002.m4v")).exists();
    }

    private PhotoUploadService newPhotoUploadService(String inputRoot) {
        return newPhotoUploadService(inputRoot, new MomentlyUploadProperties(10, 2_000_000, 12_000_000));
    }

    private PhotoUploadService newPhotoUploadService(String inputRoot, MomentlyUploadProperties limits) {
        PhotoInfoPipelineProperties pipeline = new PhotoInfoPipelineProperties(
            "python3",
            "../photo_exif_llm_pipeline/src/run_pipeline.py",
            inputRoot,
            uploadRoot.resolve("out").toString(),
            "http://localhost:11434",
            "dummy",
            "dummy",
            60,
            "ffmpeg",
            1.0,
            3,
            4.0,
            4,
            true,
            false
        );
        return new PhotoUploadService(pipeline, limits);
    }

    private static byte[] tinyPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }

    private static byte[] fakeIsoBmff(String brand) {
        byte[] bytes = new byte[24];
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x18;
        bytes[4] = 'f';
        bytes[5] = 't';
        bytes[6] = 'y';
        bytes[7] = 'p';
        byte[] brandBytes = brand.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(brandBytes, 0, bytes, 8, Math.min(4, brandBytes.length));
        return bytes;
    }
}
