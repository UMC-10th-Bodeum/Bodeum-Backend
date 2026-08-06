package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostLikeResponse(
        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked,

        @Schema(description = "게시글의 전체 좋아요 수", example = "12")
        int likeCount
) {
}
