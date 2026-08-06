package com.bodeum.domain.info.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "후기 도움돼요 토글 응답")
public record InfoHelpfulToggleResponse(
        @Schema(description = "도움돼요 등록 여부 (true: 도움돼요 설정됨, false: 취소됨)", example = "true")
        boolean isHelpful,

        @Schema(description = "최종 도움돼요 수", example = "15")
        int helpfulCount
) {
    public static InfoHelpfulToggleResponse of(boolean isHelpful, int helpfulCount) {
        return new InfoHelpfulToggleResponse(isHelpful, helpfulCount);
    }
}