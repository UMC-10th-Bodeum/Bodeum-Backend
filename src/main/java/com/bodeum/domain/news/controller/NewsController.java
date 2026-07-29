package com.bodeum.domain.news.controller;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.dto.response.NewsDetailResponse;
import com.bodeum.domain.news.dto.response.NewsListResponse;
import com.bodeum.domain.news.dto.response.NewsScrapResponse;
import com.bodeum.domain.news.dto.response.NewsSearchSuggestionsResponse;
import com.bodeum.domain.news.dto.response.RelatedRecruitingNewsResponse;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.service.NewsQueryService;
import com.bodeum.domain.news.service.NewsScrapService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "소식 조회 API")
@SecurityRequirements
public class NewsController {

    private final NewsQueryService newsQueryService;
    private final NewsScrapService newsScrapService;

    @GetMapping
    @Operation(summary = "소식 목록 조회", description = "최신 소식과 모집 상태별 소식을 조회한다.")
    public ApiResponse<NewsListResponse> getNews(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(
                    description = "정렬 기준",
                    example = "latest",
                    schema = @Schema(allowableValues = "latest")
            )
            @RequestParam(defaultValue = "latest")
            @Pattern(regexp = "latest") String sort,
            @Parameter(description = "지역", example = "서울")
            @RequestParam(required = false) @Size(max = 50) String region,
            @Parameter(description = "카테고리 코드", example = "EDUCATION_SEMINAR")
            @RequestParam(required = false) NewsCategoryCode category,
            @Parameter(description = "모집 상태", example = "RECRUITING")
            @RequestParam(required = false) NewsStatus status
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsQueryService.getNews(page, size, region, category, status)
        );
    }

    @GetMapping("/search")
    @Operation(summary = "소식 검색", description = "검색어와 필터를 기준으로 소식 목록을 조회한다.")
    public ApiResponse<NewsListResponse> searchNews(
            @Parameter(description = "검색어", example = "봉사")
            @RequestParam @NotBlank @Size(max = 100) String keyword,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "지역", example = "서울")
            @RequestParam(required = false) @Size(max = 50) String region,
            @Parameter(description = "카테고리 코드", example = "EDUCATION_SEMINAR")
            @RequestParam(required = false) NewsCategoryCode category,
            @Parameter(description = "모집 상태", example = "RECRUITING")
            @RequestParam(required = false) NewsStatus status
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsQueryService.searchNews(keyword, page, size, region, category, status)
        );
    }

    @GetMapping("/search/suggestions")
    @Operation(
            summary = "소식 검색어 자동완성 조회",
            description = "입력한 검색어가 포함된 활성 소식 제목을 자동완성 검색어로 조회한다."
    )
    public ApiResponse<NewsSearchSuggestionsResponse> getNewsSearchSuggestions(
            @Parameter(description = "자동완성 검색어", example = "봉")
            @RequestParam @NotBlank @Size(max = 50) String keyword,
            @Parameter(description = "조회 개수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsQueryService.getSearchSuggestions(keyword, size)
        );
    }

    @GetMapping("/{newsId}")
    @Operation(summary = "소식 상세 조회", description = "특정 소식의 상세 정보를 조회한다.")
    public ApiResponse<NewsDetailResponse> getNewsDetail(
            @Parameter(description = "소식 ID", example = "1")
            @PathVariable @Positive Long newsId,
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsQueryService.getNewsDetail(userId, newsId)
        );
    }

    @GetMapping("/{newsId}/related")
    @Operation(
            summary = "같은 지역 모집 중 소식 조회",
            description = "현재 소식을 제외하고 같은 시 단위 지역에서 모집 중인 소식을 최신순으로 조회한다."
    )
    public ApiResponse<List<RelatedRecruitingNewsResponse>> getRelatedRecruitingNews(
            @Parameter(description = "현재 조회 중인 소식 ID", example = "1")
            @PathVariable @Positive Long newsId,
            @Parameter(description = "조회 개수", example = "5")
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int size
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsQueryService.getRelatedRecruitingNews(newsId, size)
        );
    }

    @PostMapping("/{newsId}/scrap")
    @Operation(summary = "소식 스크랩 토글", description = "로그인 사용자의 소식 스크랩 상태를 전환한다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<NewsScrapResponse> toggleNewsScrap(
            @Parameter(description = "소식 ID", example = "1")
            @PathVariable @Positive Long newsId,
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                newsScrapService.toggleScrap(userId, newsId)
        );
    }
}
