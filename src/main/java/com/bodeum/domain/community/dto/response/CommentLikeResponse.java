package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentLikeResponse(
        @Schema(description = "현재 사용자의 댓글 좋아요 여부", example = "true")
        boolean isLiked,

        @Schema(description = "댓글의 전체 좋아요 수", example = "3")
        int likeCount
) {
}
