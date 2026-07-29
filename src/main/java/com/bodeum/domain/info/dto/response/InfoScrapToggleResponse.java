package com.bodeum.domain.info.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정보 스크랩 토글 응답")
public record InfoScrapToggleResponse(
        @Schema(description = "스크랩 등록 여부 (true: 스크랩됨, false: 스크랩 해제됨)", example = "true")
        boolean isScrapped,

        @Schema(description = "최종 스크랩 수", example = "42")
        int scrapCount
) {
    public static InfoScrapToggleResponse of(boolean isScrapped, int scrapCount) {
        return new InfoScrapToggleResponse(isScrapped, scrapCount);
    }
}