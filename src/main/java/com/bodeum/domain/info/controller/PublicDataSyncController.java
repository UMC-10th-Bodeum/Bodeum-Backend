package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.service.publicDataService.PublicDataSyncFacadeService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sync")
public class PublicDataSyncController {

    private final PublicDataSyncFacadeService publicDataSyncFacadeService;

    /**
     * 1. 전체 공공데이터 API 수동 통합 비동기 동기화
     */
    @PostMapping("/all")
    public ApiResponse<String> syncAllPublicData() {
        // 백그라운드 전용 스레드(Sync-Thread)에서 13개 API를 순차 동기화
        publicDataSyncFacadeService.syncAllAsync();
        return ApiResponse.of(GeneralSuccessCode.OK, "전체 공공데이터 비동기 동기화 백그라운드 작업이 시작되었습니다.");
    }

    /**
     * 2. 대분류 카테고리별 선택 비동기 동기화 (예: /api/v1/admin/sync/main-category/WELFARE)
     */
    @PostMapping("/main-category/{mainCategory}")
    public ApiResponse<String> syncByMainCategory(@PathVariable MainCategory mainCategory) {
        publicDataSyncFacadeService.syncByMainCategoryAsync(mainCategory);
        return ApiResponse.of(GeneralSuccessCode.OK, mainCategory + " 카테고리 비동기 동기화 작업이 시작되었습니다.");
    }

    /**
     * 3. 특정 카테고리 ID 단건 선택 비동기 동기화 (예: /api/v1/admin/sync/category/4)
     */
    @PostMapping("/category/{categoryId}")
    public ApiResponse<String> syncByCategoryId(@PathVariable Long categoryId) {
        publicDataSyncFacadeService.syncByCategoryIdAsync(categoryId);
        return ApiResponse.of(GeneralSuccessCode.OK, "카테고리 ID: " + categoryId + " 비동기 동기화 작업이 시작되었습니다.");
    }
}