package com.bodeum.domain.community.controller;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostLikeResponse;
import com.bodeum.domain.community.dto.response.PostListItemResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.dto.response.PostScrapResponse;
import com.bodeum.domain.community.dto.response.PostSearchSuggestionsResponse;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostListSortType;
import com.bodeum.domain.community.service.PostListService;
import com.bodeum.domain.community.service.PostQueryFacade;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community Post", description = "커뮤니티 게시글 작성·수정·삭제·상세 조회 API")
@RestController
@RequestMapping("/api/v1/community/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;
    private final PostQueryFacade postQueryFacade;
    private final PostListService postListService;

    @Operation(
            summary = "게시글 목록 조회 및 검색",
            description = "전체 게시글 또는 제목·본문에 검색어가 포함된 게시글을 14개씩 조회한다."
    )
    @GetMapping
    public ApiResponse<Page<PostListItemResponse>> getPosts(
            @LoginUser Long userId,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(
                    description = "정렬 기준: latest, view, scrap, comment. "
                            + "미지정 시 로그인 사용자는 latest, 비회원은 view",
                    example = PostListSortType.LOGGED_IN_DEFAULT_SORT_VALUE
            )
            @RequestParam(required = false) String sort,
            @Parameter(description = "제목 또는 본문 검색어. 생략하거나 공백이면 전체 조회")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "게시판 카테고리 코드. 생략하면 전체 카테고리 조회")
            @RequestParam(required = false) PostBoardType categoryCode
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                postListService.getPosts(userId, page, sort, keyword, categoryCode)
        );
    }

    @Operation(
            summary = "게시글 검색어 추천 조회",
            description = "입력한 검색어가 제목 또는 본문에 포함된 활성 게시글의 제목을 추천어로 조회한다."
    )
    @GetMapping("/search/suggestions")
    public ApiResponse<PostSearchSuggestionsResponse> getSearchSuggestions(
            @Parameter(description = "추천 검색어(2자 이상)", example = "자폐스펙트럼")
            @RequestParam @NotBlank @Size(min = 2, max = 50) String keyword,
            @Parameter(description = "조회 개수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                postListService.getSearchSuggestions(keyword, size)
        );
    }

    @Operation(summary = "게시글 작성", description = "커뮤니티 게시글과 장애 유형·해시태그·이미지를 등록한다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @LoginUser Long userId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, postService.createPost(userId, request));
    }

    @Operation(summary = "게시글 수정", description = "작성자가 게시글과 연관 태그·이미지를 수정한다.")
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @LoginUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postService.updatePost(userId, postId, request));
    }

    @Operation(summary = "게시글 삭제", description = "작성자가 게시글을 논리 삭제한다.")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        postService.deletePost(userId, postId);
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글과 장애 유형·해시태그·이미지를 상세 조회한다.")
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postQueryFacade.getPost(userId, postId));
    }

    @Operation(summary = "게시글 좋아요 등록")
    @PutMapping("/{postId}/likes")
    public ApiResponse<PostLikeResponse> likePost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postService.likePost(userId, postId));
    }

    @Operation(summary = "게시글 좋아요 취소")
    @DeleteMapping("/{postId}/likes")
    public ApiResponse<PostLikeResponse> unlikePost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postService.unlikePost(userId, postId));
    }

    @Operation(summary = "게시글 스크랩 등록")
    @PutMapping("/{postId}/scraps")
    public ApiResponse<PostScrapResponse> scrapPost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postService.scrapPost(userId, postId));
    }

    @Operation(summary = "게시글 스크랩 취소")
    @DeleteMapping("/{postId}/scraps")
    public ApiResponse<PostScrapResponse> unscrapPost(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postService.unscrapPost(userId, postId));
    }
}
