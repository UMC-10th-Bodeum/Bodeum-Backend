package com.bodeum.domain.community.dto.request;

import com.bodeum.domain.community.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @Schema(description = "수정할 댓글 내용", example = "댓글 내용을 수정했습니다.")
        @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
        @Size(max = Comment.CONTENT_MAX_LENGTH, message = "댓글 내용은 1,000자 이하로 입력해주세요.")
        String content
) {
}
