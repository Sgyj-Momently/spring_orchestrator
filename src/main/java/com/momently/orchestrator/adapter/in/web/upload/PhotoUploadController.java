package com.momently.orchestrator.adapter.in.web.upload;

import com.momently.orchestrator.config.MomentlyUploadProperties;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 콘솔에서 선택한 로컬 사진과 동영상을 서버 입력 폴더로 옮기는 API다.
 *
 * <p>JWT 인증 컨텍스트가 있어야 호출된다.</p>
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class PhotoUploadController {

    private final PhotoUploadService photoUploadService;
    private final MomentlyUploadProperties uploadProperties;

    public PhotoUploadController(PhotoUploadService photoUploadService, MomentlyUploadProperties uploadProperties) {
        this.photoUploadService = photoUploadService;
        this.uploadProperties = uploadProperties;
    }

    /**
     * 콘솔 화면 검증에 필요한 현재 서버 업로드 정책을 반환한다.
     */
    @GetMapping("/config")
    public ResponseEntity<PhotoUploadConfigResponse> uploadConfig() {
        return ResponseEntity.ok(new PhotoUploadConfigResponse(
            uploadProperties.maxFiles(),
            uploadProperties.maxBytesPerFile(),
            uploadProperties.maxTotalBytes(),
            PhotoUploadService.ALLOWED_EXTENSIONS
        ));
    }

    /**
     * 여러 미디어 파일을 한 번에 올린 뒤, 워크플로 생성에 사용할 프로젝트 식별자를 돌려준다.
     *
     * @param files 동일 이름 파트가 반복 가능한 multipart 파일 목록
     */
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadResponse> uploadMedia(@RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(photoUploadService.saveUploadedMedia(files));
    }

    /**
     * 기존 콘솔/클라이언트 호환을 위한 이미지 업로드 경로다.
     *
     * <p>저장 정책은 {@code /media}와 동일하므로, 새 클라이언트는 {@code /media}를 우선 사용한다.</p>
     *
     * @param files 동일 이름 파트가 반복 가능한 multipart 파일 목록
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadResponse> uploadImages(@RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(photoUploadService.saveUploadedMedia(files));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PhotoUploadErrorResponse> handleBadArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new PhotoUploadErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<PhotoUploadErrorResponse> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new PhotoUploadErrorResponse(exception.getMessage()));
    }
}
