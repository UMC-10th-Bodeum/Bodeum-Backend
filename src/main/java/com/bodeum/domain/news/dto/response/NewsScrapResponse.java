package com.bodeum.domain.news.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NewsScrapResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,

        @Schema(description = "현재 사용자의 스크랩 여부", example = "true")
        boolean scrapped,

        @Schema(description = "소식의 전체 스크랩 수", example = "12")
        long scrapCount
) {
}
