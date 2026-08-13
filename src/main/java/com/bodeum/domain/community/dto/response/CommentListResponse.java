package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CommentListResponse(
        @Schema(description = "활성 댓글과 답글의 전체 개수. 삭제 댓글 자리표시자는 제외", example = "12")
        int totalCount,

        @Schema(
                description = "최상위 댓글 목록. 삭제된 댓글은 개인정보와 원문을 숨긴 DELETED 자리표시자로 "
                        + "포함되며 기존 replies 계층을 유지합니다."
        )
        List<CommentResponse> comments
) {

    public CommentListResponse {
        comments = List.copyOf(comments);
    }
}
