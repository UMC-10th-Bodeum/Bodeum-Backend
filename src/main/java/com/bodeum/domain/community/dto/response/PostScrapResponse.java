package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostScrapResponse(
        @Schema(description = "현재 사용자의 스크랩 여부", example = "true")
        boolean isScrapped,

        @Schema(description = "게시글의 전체 스크랩 수", example = "7")
        int scrapCount
) {
}
