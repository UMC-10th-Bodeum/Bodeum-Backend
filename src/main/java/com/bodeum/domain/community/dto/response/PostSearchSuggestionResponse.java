package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostSearchSuggestionResponse(
        @Schema(description = "검색어와 일치하는 게시글 제목", example = "자폐스펙트럼 아이의 치료 기록")
        String text,

        @Schema(description = "추천 검색어 유형", example = "POST_TITLE")
        Type type
) {

    public static PostSearchSuggestionResponse fromTitle(String title) {
        return new PostSearchSuggestionResponse(title, Type.POST_TITLE);
    }

    public enum Type {
        POST_TITLE
    }
}
