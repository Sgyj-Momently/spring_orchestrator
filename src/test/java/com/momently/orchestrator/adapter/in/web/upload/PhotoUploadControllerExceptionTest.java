package com.momently.orchestrator.adapter.in.web.upload;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 업로드 컨트롤러의 예외 응답 매핑을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PhotoUploadControllerExceptionTest {

    @Mock
    private PhotoUploadService photoUploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PhotoUploadController(photoUploadService)).build();
    }

    @Test
    @DisplayName("IllegalStateException은 503과 본문 메시지로 매핑된다")
    void mapsIllegalStateToServiceUnavailable() throws Exception {
        when(photoUploadService.saveUploadedImages(anyList()))
            .thenThrow(new IllegalStateException("입력 루트 없음"));

        MockMultipartFile part = new MockMultipartFile(
            "files",
            "1.png",
            "image/png",
            new byte[] {1}
        );

        mockMvc.perform(multipart("/api/v1/uploads/images").file(part))
            .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .andExpect(jsonPath("$.error").value("입력 루트 없음"));
    }
}
