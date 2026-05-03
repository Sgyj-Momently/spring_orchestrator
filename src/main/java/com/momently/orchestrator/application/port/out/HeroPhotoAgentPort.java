package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;

/**
 * 대표 사진 선택 단계를 애플리케이션 계층에서 호출하기 위한 아웃바운드 포트다.
 *
 * <p>이 포트는 오케스트레이터가 대표 사진 선택 에이전트의 HTTP 주소, 입력 DTO, 모델 세부사항을
 * 직접 알지 못하게 하는 경계다. 구현체는 그룹화 결과 아티팩트와 사진 정보 bundle을 읽어
 * 대표 사진 선택 에이전트가 이해하는 계약으로 변환하고, 오케스트레이션에 필요한 최소 결과만
 * {@link HeroPhotoResult}로 변환해야 한다.</p>
 */
public interface HeroPhotoAgentPort {

    /**
     * 그룹 결과를 바탕으로 그룹별 대표 사진을 선택한다.
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @param photoInfoResult 사진 정보 추출 단계가 남긴 bundle 아티팩트 참조
     * @param photoGroupingResult 사진 그룹화 단계가 남긴 그룹화 결과 아티팩트 참조
     * @return 대표 사진 선택 결과 요약과 아티팩트 파일 경로
     */
    HeroPhotoResult selectHeroPhotos(
        String projectId,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult
    );
}

