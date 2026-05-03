package com.momently.orchestrator.adapter.in.web.upload;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.momently.orchestrator.security.JwtService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 업로드 API의 HTTP 계약을 검증한다.
 */
@WebMvcTest(PhotoUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
class PhotoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoUploadService photoUploadService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("멀티파트 업로드가 성공하면 프로젝트 식별자를 반환한다")
    void returnsProjectIdOnSuccess() throws Exception {
        when(photoUploadService.saveUploadedImages(anyList()))
            .thenReturn(new PhotoUploadResponse("u_deadbeefdeadbeefdeadbeefdeadbeef", 2, 42L));

        MockMultipartFile a = new MockMultipartFile(
            "files",
            "1.png",
            "image/png",
            new byte[] {1}
        );

        MockMultipartFile b = new MockMultipartFile(
            "files",
            "2.png",
            "image/png",
            new byte[] {2}
        );

        mockMvc.perform(
                multipart("/api/v1/uploads/images")
                    .file(a)
                    .file(b))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("u_deadbeefdeadbeefdeadbeefdeadbeef"))
            .andExpect(jsonPath("$.savedCount").value(2))
            .andExpect(jsonPath("$.bytesTotal").value(42));
    }

    @Test
    @DisplayName("검증 실패 시 400과 에러 메시지를 반환한다")
    void returnsBadRequestBody() throws Exception {
        when(photoUploadService.saveUploadedImages(anyList()))
            .thenThrow(new IllegalArgumentException("테스트 메시지"));

        MockMultipartFile part = new MockMultipartFile(
            "files",
            "1.png",
            "image/png",
            new byte[] {10}
        );

        mockMvc.perform(multipart("/api/v1/uploads/images").file(part))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("테스트 메시지"));
    }
}
