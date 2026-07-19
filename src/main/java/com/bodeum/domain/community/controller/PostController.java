package com.bodeum.domain.community.controller;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostLikeResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.dto.response.PostScrapResponse;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community Post", description = "커뮤니티 게시글 작성·수정·삭제·상세 조회 API")
@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

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
        return ApiResponse.of(GeneralSuccessCode.OK, postService.getPost(userId, postId));
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
