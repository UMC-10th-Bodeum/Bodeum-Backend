package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.repository.InfoRegionRepository;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Info", description = "정보 지역 드롭다운 관련 API")
@RestController
@RequestMapping("/api/v1/info-regions")
@RequiredArgsConstructor
public class InfoRegionController {

    private final InfoRegionRepository infoRegionRepository;

    @Operation(
            summary = "시/도 목록 조회",
            description = "프론트엔드 2단 지역 선택 드롭다운용 시/도 목록을 조회한다."
    )
    @SecurityRequirements
    @GetMapping("/sido")
    public ApiResponse<List<String>> getSidoList() {
        List<String> result = infoRegionRepository.findDistinctSido();
        return ApiResponse.of(GeneralSuccessCode.OK, result);
    }

    @Operation(
            summary = "시/군/구 목록 조회",
            description = "선택한 시/도에 해당하는 시/군/구 목록을 조회한다."
    )
    @SecurityRequirements
    @GetMapping("/sigungu")
    public ApiResponse<List<String>> getSigunguList(
            @Parameter(description = "시/도 명칭", example = "서울특별시")
            @RequestParam("sido") String sido
    ) {
        List<String> result = infoRegionRepository.findSigunguBySido(sido);
        return ApiResponse.of(GeneralSuccessCode.OK, result);
    }
}