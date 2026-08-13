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
        @Schema(description = "작성자 ID. 삭제된 댓글 또는 탈퇴 회원이면 null", nullable = true)
        Long authorId,
        @Schema(
                description = "작성자 표시명. 닉네임이 있으면 닉네임, 없으면 게시글 내 '익명 N', "
                        + "탈퇴 회원이면 '탈퇴한 사용자', 삭제된 댓글이면 null",
                nullable = true
        )
        String authorNickname,
        @Schema(
                description = "작성자 프로필 이미지 URL. 삭제된 댓글, 탈퇴 회원 또는 이미지가 없으면 null",
                nullable = true
        )
        String profileImageUrl,
        @Schema(description = "현재 사용자가 작성한 댓글인지 여부. 삭제된 댓글이면 false")
        boolean isMine,
        @Schema(description = "댓글 내용. 삭제된 댓글이면 '삭제된 댓글입니다'", example = "댓글 내용")
        String content,
        @Schema(description = "채택 여부. 삭제된 댓글이면 false")
        boolean isAccepted,
        @Schema(description = "좋아요 수. 삭제된 댓글이면 0")
        int likeCount,
        @Schema(description = "현재 사용자의 좋아요 여부. 삭제된 댓글이면 false")
        boolean isLiked,
        @Schema(description = "댓글 상태. 삭제된 댓글 자리표시자는 DELETED", example = "ACTIVE")
        CommentStatus status,
        Instant createdAt,
        @Schema(description = "댓글 최종 수정 일시")
        Instant updatedAt,
        @Schema(description = "현재 댓글에 달린 답글 목록. 동일한 구조로 깊이 제한 없이 중첩됩니다.")
        List<CommentResponse> replies
) {

    public static final String DELETED_CONTENT = "삭제된 댓글입니다";

    public static CommentResponse of(
            Comment comment,
            Long viewerId,
            boolean liked,
            List<CommentResponse> replies
    ) {
        Long parentCommentId = comment.getParent() == null ? null : comment.getParent().getId();
        return of(comment, parentCommentId, viewerId, liked, false, null, null, replies);
    }

    public static CommentResponse of(
            Comment comment,
            Long parentCommentId,
            Long viewerId,
            boolean liked,
            boolean authorWithdrawn,
            String activeAuthorNickname,
            String activeAuthorProfileImageUrl,
            List<CommentResponse> replies
    ) {
        if (comment.getStatus() == CommentStatus.DELETED || comment.isDeleted()) {
            return deleted(comment, parentCommentId, replies);
        }

        Long authorId = authorWithdrawn ? null : comment.getUserId();
        String authorNickname = authorWithdrawn
                ? WithdrawalConstants.WITHDRAWN_DISPLAY_NAME
                : activeAuthorNickname;
        String profileImageUrl = authorWithdrawn ? null : activeAuthorProfileImageUrl;

        return new CommentResponse(
                comment.getId(),
                parentCommentId,
                authorId,
                authorNickname,
                profileImageUrl,
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

    public static CommentResponse deleted(
            Comment comment,
            Long parentCommentId,
            List<CommentResponse> replies
    ) {
        return new CommentResponse(
                comment.getId(),
                parentCommentId,
                null,
                null,
                null,
                false,
                DELETED_CONTENT,
                false,
                0,
                false,
                CommentStatus.DELETED,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                List.copyOf(replies)
        );
    }
}
