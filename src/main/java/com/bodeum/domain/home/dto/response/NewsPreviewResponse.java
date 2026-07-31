package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.region.entity.Region;

public record NewsPreviewResponse(
        Long newsId,
        String regionLevel1,
        String regionLevel2,
        String title,
        long likeCount,
        long viewCount
) {
    public static NewsPreviewResponse from(News news, Region region) {
        return new NewsPreviewResponse(
                news.getId(),
                region != null ? region.getRegionLevel1() : null,
                region != null ? region.getRegionLevel2() : null,
                news.getTitle(),
                news.getScrapCount(),
                news.getViewCount()
        );
    }
}
