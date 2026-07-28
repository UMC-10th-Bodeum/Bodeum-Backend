package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.news.entity.News;

public record NewsPreviewResponse(
        Long newsId,
        Long region,
        String title,
        long likeCount,
        long viewCount
) {
    public static NewsPreviewResponse from(News news) {
        return new NewsPreviewResponse(
                news.getId(),
                news.getRegionId(),
                news.getTitle(),
                news.getScrapCount(),
                news.getViewCount()
        );
    }
}
