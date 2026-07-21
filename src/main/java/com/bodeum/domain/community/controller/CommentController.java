package com.bodeum.domain.community.controller;

import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.UpdateCommentRequest;
import com.bodeum.domain.community.dto.response.CommentLikeResponse;
import com.bodeum.domain.community.dto.response.CommentListResponse;
import com.bodeum.domain.community.dto.response.CommentResponse;
import com.bodeum.domain.community.service.CommentService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
@Tag(name = "Community Comment", description = "커뮤니티 댓글 및 중첩 답글 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 등록")
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @LoginUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, commentService.createComment(userId, postId, request));
    }

    @Operation(summary = "중첩 답글 등록", description = "일반 댓글과 답글 모두 새로운 답글의 부모가 될 수 있다.")
    @PostMapping("/comments/{parentCommentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createReply(
            @LoginUser Long userId,
            @PathVariable Long parentCommentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.CREATED,
                commentService.createReply(userId, parentCommentId, request)
        );
    }

    @Operation(
            summary = "게시글 댓글 트리 조회",
            description = "인증 여부와 관계없이 활성 댓글과 답글을 조회하며, 삭제된 댓글은 응답에서 제외한다."
    )
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<CommentListResponse> getComments(
            @LoginUser Long userId,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, commentService.getComments(userId, postId));
    }

    @Operation(summary = "댓글 수정")
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @LoginUser Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, commentService.updateComment(userId, commentId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자가 댓글을 논리 삭제한다.")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @LoginUser Long userId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userId, commentId);
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }

    @Operation(summary = "댓글 좋아요 등록")
    @PutMapping("/comments/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> likeComment(
            @LoginUser Long userId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, commentService.likeComment(userId, commentId));
    }

    @Operation(summary = "댓글 좋아요 취소")
    @DeleteMapping("/comments/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> unlikeComment(
            @LoginUser Long userId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, commentService.unlikeComment(userId, commentId));
    }
}
