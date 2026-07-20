package com.bodeum.domain.info.controller;

import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.infrastructure.scheduler.OpenApiFetchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/openapi")
public class AdminOpenApiController {

    private final OpenApiFetchScheduler openApiFetchScheduler;

    // 외부 API 수집 배치를 즉시 강제 구동
    @PostMapping("/fetch-trigger")
    public ApiResponse<Void> triggerOpenApiFetch() {
        log.info("[Admin API] 오픈 API 수집 배치 수동 트리거 시작");

        // 스케줄러의 수집 메서드 직접 호출
        openApiFetchScheduler.fetchAllOpenApiData();

        log.info("[Admin API] 오픈 API 수집 배치 수동 트리거 완료");
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}