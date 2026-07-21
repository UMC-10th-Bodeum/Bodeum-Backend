package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.dto.request.CreateInfoReviewRequest;
import com.bodeum.domain.info.dto.request.UpdateInfoReviewRequest;
import com.bodeum.domain.info.dto.response.InfoReviewResponse;
import com.bodeum.domain.info.service.InfoReviewService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.BaseErrorCode;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Info", description = "정보 도메인 관련 API")
@RestController
@RequestMapping("/api/v1/info-items/{infoItemId}/reviews")
@RequiredArgsConstructor
public class InfoReviewController {

    private final InfoReviewService infoReviewService;

    @Operation(
            summary = "정보 후기 목록 조회",
            description = "특정 정보 항목에 대한 후기 목록을 페이징 조회한다. 비회원 및 로그인 사용자 모두 접근 가능하다."
    )
    @SecurityRequirements
    @GetMapping
    public ApiResponse<Page<InfoReviewResponse>> getReviews(
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId,
            @Parameter(description = "페이징 파라미터 (page, size, sort)")
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<InfoReviewResponse> response = infoReviewService.getReviews(infoItemId, pageable);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }

    @Operation(
            summary = "정보 후기 작성",
            description = "현재 로그인한 사용자가 특정 정보 항목에 대한 후기(별점, 본인 글, 이미지 URL 목록)를 등록한다."
    )
    @PostMapping
    public ApiResponse<InfoReviewResponse> createReview(
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId,
            @LoginUser Long userId,
            @Valid @RequestBody CreateInfoReviewRequest request
    ) {
        InfoReviewResponse response = infoReviewService.createReview(infoItemId, userId, request);
        return ApiResponse.of(GeneralSuccessCode.CREATED, response);
    }

    @Operation(
            summary = "정보 후기 수정",
            description = "현재 로그인한 사용자가 본인이 작성한 정보 후기를 수정한다. 별점, 내용, 이미지 URL 목록을 교체한다."
    )
    @PatchMapping("/{infoReviewId}")
    public ApiResponse<InfoReviewResponse> updateReview(
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId,
            @Parameter(description = "정보 후기 ID", example = "10")
            @PathVariable Long infoReviewId,
            @LoginUser Long userId,
            @Valid @RequestBody UpdateInfoReviewRequest request
    ) {
        InfoReviewResponse response = infoReviewService.updateReview(infoReviewId, userId, request);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }

    @Operation(
            summary = "정보 후기 삭제",
            description = "현재 로그인한 사용자가 본인이 작성한 정보 후기를 삭제한다."
    )
    @DeleteMapping("/{infoReviewId}")
    public ApiResponse<Void> deleteReview(
            @Parameter(description = "정보 항목 ID", example = "1")
            @PathVariable Long infoItemId,
            @Parameter(description = "정보 후기 ID", example = "10")
            @PathVariable Long infoReviewId,
            @LoginUser Long userId
    ) {
        infoReviewService.deleteReview(infoReviewId, userId);
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }

    /**
     * JSON 파싱 및 데이터 타입 변환 실패 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST;
        String message = "요청 데이터 형식이 올바르지 않습니다. (입력값의 타입을 확인해주세요)";
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode, message));
    }
}