package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 콘솔에서 여러 장의 사진을 업로드할 때 사용하는 제한 값이다.
 *
 * <p>실제 저장 위치는 항상 {@code agents.photo-info.pipeline.input-root} 하위의
 * 서버가 발급한 전용 디렉터리로만 한정한다.</p>
 *
 * @param maxFiles 한 요청에 허용할 최대 파일 개수
 * @param maxBytesPerFile 파일 하나당 최대 바이트 수
 * @param maxTotalBytes 한 요청 전체 합산 최대 바이트 수
 */
@ConfigurationProperties(prefix = "momently.upload")
public record MomentlyUploadProperties(int maxFiles, long maxBytesPerFile, long maxTotalBytes) {

    public MomentlyUploadProperties {
        if (maxFiles <= 0) {
            maxFiles = 40;
        }
        if (maxBytesPerFile <= 0) {
            maxBytesPerFile = 25L * 1024 * 1024;
        }
        if (maxTotalBytes <= 0) {
            maxTotalBytes = 120L * 1024 * 1024;
        }
    }
}
