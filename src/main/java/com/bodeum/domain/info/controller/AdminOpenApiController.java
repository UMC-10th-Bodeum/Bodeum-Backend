package com.bodeum.domain.info.controller;

import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.infrastructure.scheduler.OpenApiFetchScheduler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final Executor taskExecutor;

    // 외부 API 수집 배치를 즉시 강제 구동
    @PostMapping("/fetch-trigger")
    public ApiResponse<Void> triggerOpenApiFetch() {
        log.info("[Admin API] 오픈 API 수집 배치 수동 트리거 시작");

        // 공용 ForkJoinPool 대신 전용 TaskExecutor를 전달하여 비동기 실행 (스레드 고갈 방지)
        CompletableFuture.runAsync(openApiFetchScheduler::fetchAllOpenApiData, taskExecutor);

        log.info("[Admin API] 오픈 API 수집 배치 수동 트리거 요청 완료 (백그라운드 실행 중)");
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}