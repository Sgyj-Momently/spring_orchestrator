package com.momently.orchestrator.adapter.in.web.upload;

import com.momently.orchestrator.config.MomentlyUploadProperties;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 멀티파트 이미지를 파이프라인 입력 디렉터리로만 옮긴다.
 *
 * <p>원본 파일명은 무시하고 안전한 순번 파일명만 쓴다. 경로는 항상 설정된 input-root 이하의
 * 서버가 새로 만든 하위 폴더로 제한한다.</p>
 *
 * <p>확장자·MIME은 조작 가능하므로, 저장 후 헤더(매직) 검사에 더해 JPG/PNG는
 * {@link ImageIO} 한 장 디코드를 시도해 실제 비트맵 여부까지 확인한다. WEBP·HEIC 등은 표준 실행 환경에
 * 디코더가 없을 수 있어 헤더 검증 단계까지로 둔다.</p>
 */
@Service
public class PhotoUploadService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "heic", "heif", "webp");

    private final PhotoInfoPipelineProperties pipelineProperties;
    private final MomentlyUploadProperties uploadProperties;

    public PhotoUploadService(
        PhotoInfoPipelineProperties pipelineProperties,
        MomentlyUploadProperties uploadProperties
    ) {
        this.pipelineProperties = pipelineProperties;
        this.uploadProperties = uploadProperties;
    }

    /**
     * 새 프로젝트 폴더를 만들고 업로드된 이미지를 저장한다.
     *
     * @return 새 {@code projectId}(폴더 이름)
     * @throws IllegalArgumentException 검증 실패 또는 스트림 오류 시
     */
    public PhotoUploadResponse saveUploadedImages(List<MultipartFile> files) {
        if (files == null || files.stream().noneMatch(file -> file != null && !file.isEmpty())) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다.");
        }

        List<MultipartFile> nonEmpty = files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .toList();

        if (nonEmpty.size() > uploadProperties.maxFiles()) {
            throw new IllegalArgumentException(
                "한 번에 올릴 수 있는 장 수는 최대 " + uploadProperties.maxFiles() + "장입니다."
            );
        }

        long declaredTotal = nonEmpty.stream()
            .mapToLong(file -> Math.max(0L, file.getSize()))
            .sum();
        if (declaredTotal > uploadProperties.maxTotalBytes()) {
            throw new IllegalArgumentException("전체 업로드 용량 한도를 초과했습니다.");
        }

        Path base = Path.of(pipelineProperties.inputRoot()).toAbsolutePath().normalize();
        if (!Files.isDirectory(base)) {
            throw new IllegalStateException("사진 입력 루트가 존재하지 않습니다: " + base);
        }

        String projectId = "u_" + UUID.randomUUID().toString().replace("-", "");
        Path targetDir = base.resolve(projectId).normalize();

        if (!targetDir.startsWith(base)) {
            throw new IllegalStateException("업로드 경로를 안전하게 계산하지 못했습니다.");
        }

        try {
            Files.createDirectories(targetDir);
        } catch (IOException exception) {
            throw new IllegalStateException("업로드 폴더를 만들 수 없습니다.", exception);
        }

        long bytesTotal = 0;
        try {
            for (int i = 0; i < nonEmpty.size(); i++) {
                MultipartFile part = nonEmpty.get(i);
                String ext = normalizedExtension(part.getOriginalFilename(), part.getContentType());
                if (ext.isEmpty()) {
                    throw new IllegalArgumentException((i + 1) + "번째 파일은 지원하지 않는 형식입니다.");
                }
                if (part.getSize() > uploadProperties.maxBytesPerFile()) {
                    throw new IllegalArgumentException((i + 1) + "번째 파일이 한 장당 용량 한도를 넘습니다.");
                }

                String fileName = String.format(Locale.US, "%04d.%s", i + 1, ext);
                Path dest = targetDir.resolve(fileName).normalize();
                if (!dest.startsWith(targetDir)) {
                    throw new IllegalStateException("파일 경로가 업로드 폴더를 벗어났습니다.");
                }

                long written = transferLimited(part.getInputStream(), dest, uploadProperties.maxBytesPerFile());
                bytesTotal += written;
                if (bytesTotal > uploadProperties.maxTotalBytes()) {
                    throw new IllegalArgumentException("전체 업로드 용량 한도를 초과했습니다.");
                }

                int prefix = (int) Math.min(32L, Math.max(written, 0L));
                byte[] header;
                try (InputStream headerIn = Files.newInputStream(dest)) {
                    header = prefix <= 0 ? new byte[0] : headerIn.readNBytes(prefix);
                }
                if (!validateMagicBytes(header, ext)) {
                    throw new IllegalArgumentException((i + 1) + "번째 파일 내용이 이미지 형식과 맞지 않습니다.");
                }
                verifyDecodableBitmap(dest, ext, i + 1);
            }
        } catch (IOException exception) {
            deleteDirectoryQuietly(targetDir);
            throw new IllegalStateException("파일 저장에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            deleteDirectoryQuietly(targetDir);
            throw exception;
        }

        return new PhotoUploadResponse(projectId, nonEmpty.size(), bytesTotal);
    }

    private static long transferLimited(InputStream inputStream, Path dest, long maxBytes) throws IOException {
        try (InputStream in = inputStream; OutputStream out = Files.newOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("파일 크기가 한 장당 용량 한도를 넘습니다.");
                }
                out.write(buffer, 0, read);
            }
            return total;
        }
    }

    private static String normalizedExtension(String originalName, String contentType) {
        String fromName = "";
        if (originalName != null && !originalName.isBlank()) {
            String base = originalName.trim();
            int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
            if (slash >= 0 && slash < base.length() - 1) {
                base = base.substring(slash + 1);
            }
            if (base.contains("..") || base.isEmpty()) {
                return "";
            }
            int dot = base.lastIndexOf('.');
            if (dot >= 0 && dot < base.length() - 1) {
                fromName = base.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }

        String ext = fromName;
        if (ext.isEmpty() && contentType != null) {
            ext = switch (contentType.toLowerCase(Locale.ROOT).split(";")[0].trim()) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/heic", "image/heif" -> "heic";
                default -> "";
            };
        }

        if (!ALLOWED_EXT.contains(ext)) {
            return "";
        }
        if ("jpeg".equals(ext)) {
            return "jpg";
        }
        if ("heif".equals(ext)) {
            return "heic";
        }
        return ext;
    }

    private static boolean validateMagicBytes(byte[] header, String ext) {
        if (header.length < 3) {
            return false;
        }
        return switch (ext) {
            case "jpg" -> header.length >= 3
                && (header[0] & 0xff) == 0xff
                && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff;
            case "png" -> header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4e
                && header[3] == 0x47
                && header[4] == 0x0d
                && header[5] == 0x0a
                && header[6] == 0x1a
                && header[7] == 0x0a;
            case "webp" -> header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
            case "heic" -> header.length >= 12 && isFtypIsoBmff(header);
            default -> false;
        };
    }

    private static boolean isFtypIsoBmff(byte[] header) {
        return header[4] == 'f'
            && header[5] == 't'
            && header[6] == 'y'
            && header[7] == 'p';
    }

    /**
     * PNG/JPEG는 확장자와 PNG 시그니처만 맞춰 둔 폴리글롯 파일을 줄이기 위해,
     * 실제 픽셀 디코드를 한 번 성공해야 통과한다.
     */
    private static void verifyDecodableBitmap(Path path, String ext, int indexOneBased) {
        if (!("png".equals(ext) || "jpg".equals(ext))) {
            return;
        }
        try (InputStream decodeIn = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(decodeIn);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalArgumentException(
                    indexOneBased + "번째 파일은 이미지로 읽을 수 없습니다.(확장자만 맞거나 손상된 파일일 수 있습니다)");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                indexOneBased + "번째 파일 이미지 내용 검증 실패",
                exception
            );
        }
    }

    private static void deleteDirectoryQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        //
                    }
                });
            }
        } catch (IOException ignored) {
            //
        }
    }
}
