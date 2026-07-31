package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.dto.request.KakaoMapUrlRequest;
import com.bodeum.domain.info.dto.response.InfoItemDetailResponse;
import com.bodeum.domain.info.dto.response.InfoItemPageResponse;
import com.bodeum.domain.info.dto.response.InfoItemShareResponse;
import com.bodeum.domain.info.dto.response.KakaoMapUrlResponse;
import com.bodeum.domain.info.service.InfoItemQueryService;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
            description = "지역 필터, 카테고리, 정렬 조건을 기반으로 정보 목록을 페이징 조회한다.\n" +
                    "- 비로그인: 전체 지역 + 기본 카테고리(기관)\n" +
                    "- 로그인(지역 미선택 시): 유저 관심 지역 + 기본 카테고리(기관)"
    )
    @GetMapping
    public ApiResponse<InfoItemPageResponse> getInfoItems(
            @LoginUser Long userId, // 비로그인 시 null, 로그인 시 userId 바인딩
            @ParameterObject @ModelAttribute InfoItemSearchCondition condition,
            @ParameterObject @PageableDefault(size = 14) Pageable pageable
    ) {
        InfoItemPageResponse result = infoItemQueryService.getInfoItems(userId, condition, pageable);
        return ApiResponse.of(GeneralSuccessCode.OK, result);
    }

    @Operation(
            summary = "정보 상세 조회",
            description = "특정 정보 항목의 상세 정보를 조회한다.\n" +
                    "- 조회 시 조회수 1 증가\n" +
                    "- 로그인 유저인 경우 해당 항목의 '스크랩 여부(isScrapped)'를 함께 반환"
    )
    @GetMapping("/{infoItemId}")
    public ApiResponse<InfoItemDetailResponse> getInfoItemDetail(
            @LoginUser Long userId, // 스크랩 여부 체크를 위해 userId 전달 (비로그인 시 null)
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable("infoItemId") Long infoItemId
    ) {
        InfoItemDetailResponse response = infoItemQueryService.getInfoItemDetail(userId, infoItemId);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }

    @Operation(
            summary = "카카오지도 URL 생성",
            description = "정보 항목 ID를 전달받아 해당 기관/병원의 카카오지도 이동(검색) URL을 생성하여 반환한다."
    )
    @PostMapping("/kakaomap-url")
    public ApiResponse<KakaoMapUrlResponse> createKakaoMapUrl(
            @Valid @RequestBody KakaoMapUrlRequest request
    ) {
        KakaoMapUrlResponse response = infoItemQueryService.createKakaoMapUrl(request);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }

    @Operation(
            summary = "정보 공유 링크 조회",
            description = "특정 정보 상세 페이지를 공유할 수 있는 URL 정보를 조회한다."
    )
    @GetMapping("/{infoItemId}/share")
    public ApiResponse<InfoItemShareResponse> getInfoItemShareUrl(
            @Parameter(description = "정보 항목 ID", example = "42")
            @PathVariable("infoItemId") Long infoItemId
    ) {
        InfoItemShareResponse response = infoItemQueryService.getInfoItemShareUrl(infoItemId);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }
}