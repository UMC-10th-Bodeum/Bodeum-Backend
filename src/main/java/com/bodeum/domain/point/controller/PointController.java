package com.bodeum.domain.point.controller;

import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point", description = "사용자 활동 포인트 API")
@RestController
@RequestMapping("/api/v1/users/me/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(
            summary = "내 포인트 조회",
            description = "현재 로그인한 사용자의 총 포인트와 활동 유형별 적립 내역을 조회한다."
    )
    @GetMapping
    public ApiResponse<MyPointResponse> getMyPoints(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                pointService.getMyPoints(userId)
        );
    }
}
