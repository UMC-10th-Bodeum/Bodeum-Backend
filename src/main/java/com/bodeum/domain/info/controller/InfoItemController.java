package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.dto.response.InfoItemDetailResponse;
import com.bodeum.domain.info.dto.response.InfoItemPageResponse;
import com.bodeum.domain.info.dto.response.InfoItemResponse;
import com.bodeum.domain.info.service.InfoItemQueryService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Info", description = "정보 항목 관련 API")
@RestController
@RequestMapping("/api/v1/info-items")
@RequiredArgsConstructor
public class InfoItemController {

    private final InfoItemQueryService infoItemQueryService;

    @Operation(
            summary = "정보 목록 필터링 및 조회",
            description = "지역 필터, 카테고리, 정렬 조건을 기반으로 정보 목록을 페이징 조회한다. 비회원 및 로그인 사용자 모두 접근 가능하다."
    )
    @GetMapping
    public ApiResponse<InfoItemPageResponse> getInfoItems(
            @Parameter(hidden = true) @LoginUser Long userId,
            @ParameterObject @ModelAttribute InfoItemSearchCondition condition,
            @ParameterObject @PageableDefault(size = 14) Pageable pageable
    ) {
        InfoItemPageResponse result = infoItemQueryService.getInfoItems(userId, condition, pageable);
        return ApiResponse.of(GeneralSuccessCode.OK, result);
    }

    @Operation(
            summary = "정보 상세 조회",
            description = "특정 정보 항목의 상세 정보(소개글, 주소, 연락처, 운영시간, 태그 등)를 조회한다. 조회 시 조회수가 1 증가한다."
    )
    @GetMapping("/{infoItemId}")
    public ApiResponse<InfoItemDetailResponse> getInfoItemDetail(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable("infoItemId") Long infoItemId
    ) {
        InfoItemDetailResponse response = infoItemQueryService.getInfoItemDetail(userId, infoItemId);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }
}