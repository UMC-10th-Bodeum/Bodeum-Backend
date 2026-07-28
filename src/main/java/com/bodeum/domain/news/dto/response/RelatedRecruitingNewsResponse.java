package com.bodeum.domain.news.dto.response;

import com.bodeum.domain.news.entity.News;
import io.swagger.v3.oas.annotations.media.Schema;

public record RelatedRecruitingNewsResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,
        @Schema(description = "지역", example = "부산광역시 수영구")
        String region,
        @Schema(description = "제목", example = "언어치료 프로그램 이용자 모집")
        String title,
        @Schema(description = "스크랩 수", example = "142")
        long scrapCount,
        @Schema(description = "조회 수", example = "1204")
        long viewCount
) {

    public static RelatedRecruitingNewsResponse of(News news, String region) {
        return new RelatedRecruitingNewsResponse(
                news.getId(),
                region,
                news.getTitle(),
                news.getScrapCount(),
                news.getViewCount()
        );
    }
}
