package com.bodeum.domain.home.controller;

import com.bodeum.domain.home.dto.response.*;
import com.bodeum.domain.home.service.HomeService;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Home", description = "홈 화면 API")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/v1/news/recommended")
    @Operation(summary = "추천 소식 Top5 조회")
    public ApiResponse<List<RecommendedNewsResponse>> getRecommendedNews(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, homeService.getRecommendedNews(userId));
    }

    @GetMapping("/api/v1/home/posts/preview")
    @Operation(summary = "인기글/최신글 미리보기 조회")
    public ApiResponse<List<PostPreviewResponse>> getPostsPreview(
            @Parameter(description = "정렬 기준", schema = @Schema(allowableValues = {"popular", "latest"})) @RequestParam(defaultValue = "popular") @Pattern(regexp = "popular|latest") String sort,
            @Parameter(description = "조회 개수 (1~10)") @RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit,
            @LoginUser Long userId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, homeService.getPostsPreview(sort, limit, userId));
    }

    @GetMapping("/api/v1/community/posts/recommended")
    @Operation(summary = "커뮤니티 추천게시글 조회")
    public ApiResponse<List<RecommendedPostResponse>> getRecommendedPosts(
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit,
            @LoginUser Long userId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, homeService.getRecommendedPosts(limit, userId));
    }

    @GetMapping("/api/v1/home/news/preview")
    @Operation(summary = "활동소식/지역소식 미리보기 조회")
    public ApiResponse<List<NewsPreviewResponse>> getNewsPreview(
            @RequestParam NewsType newsType,
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit,
            @LoginUser Long userId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, homeService.getNewsPreview(newsType, limit, userId));
    }

    @GetMapping("/api/v1/info-items/counts")
    @Operation(summary = "카테고리별 정보 건수 조회")
    public ApiResponse<CategoryCountResponse> getInfoItemCounts() {
        return ApiResponse.of(GeneralSuccessCode.OK, homeService.getInfoItemCounts());
    }

    @GetMapping("/api/v1/home/banner")
    @Operation(summary = "공지사항 배너 조회")
    public ResponseEntity<ApiResponse<BannerResponse>> getBanner(@LoginUser Long userId) {
        return homeService.getBanner(userId)
                .map(banner -> ResponseEntity.ok(ApiResponse.of(GeneralSuccessCode.OK, banner)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ApiResponse.of(GeneralSuccessCode.NO_CONTENT, null)));
    }
}
