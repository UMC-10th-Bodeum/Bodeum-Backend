package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PostSearchSuggestionsResponse(
        @Schema(description = "커뮤니티 게시글 검색어 추천 목록")
        List<PostSearchSuggestionResponse> suggestions
) {

    public PostSearchSuggestionsResponse {
        suggestions = List.copyOf(suggestions);
    }

    public static PostSearchSuggestionsResponse fromTitles(List<String> titles) {
        return new PostSearchSuggestionsResponse(
                titles.stream()
                        .map(PostSearchSuggestionResponse::fromTitle)
                        .toList()
        );
    }

    public static PostSearchSuggestionsResponse fromSuggestions(
            List<PostSearchSuggestionResponse> suggestions
    ) {
        return new PostSearchSuggestionsResponse(suggestions);
    }
}
