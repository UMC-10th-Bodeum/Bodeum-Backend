package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CommentListResponse(
        @Schema(description = "활성 댓글과 답글의 전체 개수", example = "12")
        int totalCount,

        @Schema(
                description = "활성 상태의 최상위 댓글 목록. 각 댓글의 replies에 활성 답글이 중첩됩니다."
        )
        List<CommentResponse> comments
) {

    public CommentListResponse {
        comments = List.copyOf(comments);
    }
}
