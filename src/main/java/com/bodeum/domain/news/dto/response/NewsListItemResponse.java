package com.bodeum.domain.news.dto.response;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record NewsListItemResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,
        @Schema(description = "제목", example = "장애아동 발달 지원 서비스")
        String title,
        @Schema(description = "요약")
        String summary,
        @Schema(description = "지역", example = "경기도 수원시")
        String region,
        @Schema(description = "카테고리 코드", example = "EDUCATION_SEMINAR")
        NewsCategoryCode categoryCode,
        @Schema(description = "카테고리 표시명", example = "교육 · 세미나")
        String categoryLabel,
        @Schema(description = "모집 상태", example = "RECRUITING")
        NewsStatus status,
        @Schema(description = "출처", example = "경기도 수원시")
        String sourceName,
        @Schema(description = "게시일", example = "2026-07-20")
        LocalDate publishedAt,
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
                news.getNewsCategory().getCode(),
                news.getNewsCategory().getLabel(),
                NewsStatus.from(news.getRecruitmentStatus()),
                news.getSourceName(),
                news.getPublishedAt().toLocalDate(),
                news.getApplyEndDate(),
                news.getThumbnailUrl(),
                news.getViewCount(),
                news.getScrapCount()
        );
    }
}
