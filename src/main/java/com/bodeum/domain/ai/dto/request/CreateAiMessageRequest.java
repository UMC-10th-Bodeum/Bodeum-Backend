package com.bodeum.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAiMessageRequest(
        @NotBlank(message = "질문 내용을 입력해 주세요.")
        @Size(max = 500, message = "질문은 500자 이하로 입력해 주세요.")
        String content
) {
}
