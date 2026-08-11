package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.service.publicDataService.PublicDataSyncFacadeService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 공공데이터 수동 동기화 트리거 (로컬/개발 전용).
 * 운영에서는 PublicDataSyncScheduler(매일 03:00)가 자동 동기화를 담당하며,
 * 관리자 권한 체계가 없어 무인증 노출을 막기 위해 prod 프로파일에서는 빈을 등록하지 않는다.
 */
@RestController
@Profile("!prod")
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sync")
public class PublicDataSyncController {

    private final PublicDataSyncFacadeService publicDataSyncFacadeService;

    @Value("${admin.secret-token}")
    private String adminSecretToken;

    // 관리자 토큰 유효성 검증
    private boolean isInvalidAdminToken(String token) {
        return token == null || !token.equals(adminSecretToken);
    }

    /**
     * 1. 전체 공공데이터 API 수동 통합 비동기 동기화
     */
    @PostMapping("/all")
    public ApiResponse<String> syncAllPublicData(
            @RequestHeader(value = "X-ADMIN-TOKEN", required = false) String token
    ) {

        if (isInvalidAdminToken(token)) {
            return ApiResponse.onFailure(InfoErrorCode.UNAUTHORIZED, null);
        }

        // 백그라운드 전용 스레드(Sync-Thread)에서 13개 API를 순차 동기화
        publicDataSyncFacadeService.syncAllAsync();
        return ApiResponse.of(GeneralSuccessCode.OK, "전체 공공데이터 비동기 동기화 백그라운드 작업이 시작되었습니다.");
    }

    /**
     * 2. 대분류 카테고리별 선택 비동기 동기화
     */
    @PostMapping("/main-category/{mainCategory}")
    public ApiResponse<String> syncByMainCategory(@PathVariable MainCategory mainCategory) {
        publicDataSyncFacadeService.syncByMainCategoryAsync(mainCategory);
        return ApiResponse.of(GeneralSuccessCode.OK, mainCategory + " 카테고리 비동기 동기화 작업이 시작되었습니다.");
    }

    /**
     * 3. 특정 카테고리 ID 단건 선택 비동기 동기화
     */
    @PostMapping("/category/{categoryId}")
    public ApiResponse<String> syncByCategoryId(@PathVariable Long categoryId) {
        publicDataSyncFacadeService.syncByCategoryIdAsync(categoryId);
        return ApiResponse.of(GeneralSuccessCode.OK, "카테고리 ID: " + categoryId + " 비동기 동기화 작업이 시작되었습니다.");
    }
}