package com.bodeum.domain.region.controller;

import com.bodeum.domain.region.dto.response.RegionResponse;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region", description = "지역 조회 API")
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(
            summary = "지역 목록 조회",
            description = "온보딩 활동 지역 선택용 전체 지역(시/도 + 시/군/구) 목록을 조회한다."
    )
    @GetMapping
    public ApiResponse<List<RegionResponse>> getRegions() {
        return ApiResponse.of(GeneralSuccessCode.OK, regionService.getRegions());
    }
}
