package com.bodeum.domain.news.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NewsSearchSuggestionsResponse(
        @Schema(description = "자동완성 검색어 목록")
        List<NewsSearchSuggestionResponse> suggestions
) {

    public NewsSearchSuggestionsResponse {
        suggestions = List.copyOf(suggestions);
    }

    public static NewsSearchSuggestionsResponse fromTitles(List<String> titles) {
        return new NewsSearchSuggestionsResponse(
                titles.stream()
                        .map(NewsSearchSuggestionResponse::fromTitle)
                        .toList()
        );
    }
}
