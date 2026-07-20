package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.enums.CommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CommentResponse(
        Long commentId,
        Long parentCommentId,
        Long authorId,
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
        return of(comment, parentCommentId, viewerId, liked, replies);
    }

    public static CommentResponse of(
            Comment comment,
            Long parentCommentId,
            Long viewerId,
            boolean liked,
            List<CommentResponse> replies
    ) {

        return new CommentResponse(
                comment.getId(),
                parentCommentId,
                comment.getUserId(),
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
