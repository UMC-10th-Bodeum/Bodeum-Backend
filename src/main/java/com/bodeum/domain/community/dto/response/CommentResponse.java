package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.global.common.constant.WithdrawalConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CommentResponse(
        Long commentId,
        Long parentCommentId,
        @Schema(description = "작성자 ID. 탈퇴 회원이면 null", nullable = true)
        Long authorId,
        @Schema(
                description = "작성자 표시명. 탈퇴 회원이면 '탈퇴한 사용자', 그 외에는 null(프론트가 authorId로 해석)",
                nullable = true
        )
        String authorNickname,
        boolean isMine,
        String content,
        boolean isAccepted,
        int likeCount,
        boolean isLiked,
        CommentStatus status,
        Instant createdAt,
        @Schema(description = "댓글 최종 수정 일시")
        Instant updatedAt,
        @Schema(description = "현재 댓글에 달린 답글 목록. 동일한 구조로 깊이 제한 없이 중첩됩니다.")
        List<CommentResponse> replies
) {

    public static CommentResponse of(
            Comment comment,
            Long viewerId,
            boolean liked,
            List<CommentResponse> replies
    ) {
        Long parentCommentId = comment.getParent() == null ? null : comment.getParent().getId();
        return of(comment, parentCommentId, viewerId, liked, false, replies);
    }

    public static CommentResponse of(
            Comment comment,
            Long parentCommentId,
            Long viewerId,
            boolean liked,
            boolean authorWithdrawn,
            List<CommentResponse> replies
    ) {
        Long authorId = authorWithdrawn ? null : comment.getUserId();
        String authorNickname = authorWithdrawn ? WithdrawalConstants.WITHDRAWN_DISPLAY_NAME : null;

        return new CommentResponse(
                comment.getId(),
                parentCommentId,
                authorId,
                authorNickname,
                Objects.equals(comment.getUserId(), viewerId),
                comment.getContent(),
                comment.isAccepted(),
                comment.getLikeCount(),
                liked,
                comment.getStatus(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                List.copyOf(replies)
        );
    }
}
