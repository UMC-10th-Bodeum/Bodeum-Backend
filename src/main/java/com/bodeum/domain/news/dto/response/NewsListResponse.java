package com.bodeum.domain.news.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NewsListResponse(
        @Schema(description = "소식 목록")
        List<NewsListItemResponse> items,
        @Schema(description = "현재 페이지(0부터 시작)", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 소식 수", example = "57")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public NewsListResponse {
        items = List.copyOf(items);
    }

    public static NewsListResponse empty(int page, int size) {
        return new NewsListResponse(List.of(), page, size, 0, 0, false);
    }
}
