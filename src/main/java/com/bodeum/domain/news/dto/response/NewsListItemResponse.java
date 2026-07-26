package com.bodeum.domain.news.dto.response;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.entity.News;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NewsListItemResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,
        @Schema(description = "제목", example = "장애아동 발달 지원 서비스")
        String title,
        @Schema(description = "요약")
        String summary,
        @Schema(description = "지역", example = "경기도 수원시")
        String region,
        @Schema(description = "카테고리", example = "SUPPORT_SERVICE")
        String category,
        @Schema(description = "모집 상태", example = "RECRUITING")
        NewsStatus status,
        @Schema(description = "출처", example = "경기도 수원시")
        String sourceName,
        @Schema(description = "게시 일시")
        LocalDateTime publishedAt,
        @Schema(description = "신청 종료일")
        LocalDate applyEndDate,
        @Schema(description = "썸네일 URL")
        String thumbnailUrl,
        @Schema(description = "조회 수", example = "10")
        long viewCount,
        @Schema(description = "스크랩 수", example = "3")
        long scrapCount
) {

    public static NewsListItemResponse of(News news, String region) {
        return new NewsListItemResponse(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                region,
                news.getNewsCategory().getName(),
                NewsStatus.from(news.getRecruitmentStatus()),
                news.getSourceName(),
                news.getPublishedAt(),
                news.getApplyEndDate(),
                news.getThumbnailUrl(),
                news.getViewCount(),
                news.getScrapCount()
        );
    }
}
