package com.bodeum.domain.news.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NewsSearchSuggestionResponse(
        @Schema(description = "추천 검색어", example = "봉사활동 참여자 모집")
        String text,

        @Schema(
                description = "자동완성 항목 유형",
                example = "NEWS_TITLE",
                allowableValues = "NEWS_TITLE"
        )
        Type type
) {

    public static NewsSearchSuggestionResponse fromTitle(String title) {
        return new NewsSearchSuggestionResponse(title, Type.NEWS_TITLE);
    }

    public enum Type {
        NEWS_TITLE
    }
}
