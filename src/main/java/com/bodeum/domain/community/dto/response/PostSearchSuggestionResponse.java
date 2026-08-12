package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostSearchSuggestionResponse(
        @Schema(
                description = "추천 게시글 제목",
                example = "자폐스펙트럼 아이의 치료 기록"
        )
        String title,

        @Schema(
                description = "게시글 본문 미리보기. 본문에서 검색어가 일치하면 검색어 주변 1~2문장, "
                        + "제목에서만 일치하면 본문 첫 1~2문장",
                example = "치료를 시작한 뒤 아이에게 나타난 변화를 기록했습니다."
        )
        String content,

        @Schema(description = "검색어가 일치한 영역", example = "POST_TITLE")
        Type type
) {

    public static PostSearchSuggestionResponse fromTitle(
            String title,
            String content
    ) {
        return new PostSearchSuggestionResponse(title, content, Type.POST_TITLE);
    }

    public static PostSearchSuggestionResponse fromContent(
            String title,
            String content
    ) {
        return new PostSearchSuggestionResponse(title, content, Type.POST_CONTENT);
    }

    public enum Type {
        POST_TITLE,
        POST_CONTENT
    }
}
