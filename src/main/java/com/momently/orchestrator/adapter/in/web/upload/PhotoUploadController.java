package com.momently.orchestrator.adapter.in.web.upload;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 콘솔에서 선택한 로컬 사진을 서버 입력 폴더로 옮기는 API다.
 *
 * <p>JWT 인증 컨텍스트가 있어야 호출된다.</p>
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class PhotoUploadController {

    private final PhotoUploadService photoUploadService;

    public PhotoUploadController(PhotoUploadService photoUploadService) {
        this.photoUploadService = photoUploadService;
    }

    /**
     * 여러 장의 이미지를 한 번에 올린 뒤, 워크플로 생성에 사용할 프로젝트 식별자를 돌려준다.
     *
     * @param files 동일 이름 파트가 반복 가능한 multipart 파일 목록
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadResponse> uploadImages(@RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(photoUploadService.saveUploadedImages(files));
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
