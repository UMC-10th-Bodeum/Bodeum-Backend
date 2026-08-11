package com.bodeum.domain.news.dto.response;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record NewsDetailResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,
        String title,
        String summary,
        String content,
        @Schema(description = "지역", example = "경기도 수원시")
        String region,
        @Schema(description = "카테고리 코드", example = "EDUCATION_SEMINAR")
        NewsCategoryCode categoryCode,
        @Schema(description = "카테고리 표시명", example = "교육 · 세미나")
        String categoryLabel,
        String sourceName,
        @Schema(description = "수집 원본 데이터의 출처 URL")
        String originalUrl,
        String thumbnailUrl,
        NewsType newsType,
        @Schema(description = "모집 상태", example = "RECRUITING")
        NewsStatus status,
        String targetAudience,
        String contact,
        String manager,
        @Schema(description = "게시일", example = "2026-07-20")
        LocalDate publishedAt,
        LocalDate programStartDate,
        LocalDate programEndDate,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        long viewCount,
        long scrapCount,
        @Schema(description = "로그인 사용자의 스크랩 여부", example = "false")
        boolean scrapped
) {

    public static NewsDetailResponse of(News news, String region, boolean scrapped) {
        return new NewsDetailResponse(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getContent(),
                region,
                news.getNewsCategory().getCode(),
                news.getNewsCategory().getLabel(),
                news.getSourceName(),
                news.getOriginalUrl(),
                news.getThumbnailUrl(),
                news.getNewsType(),
                NewsStatus.from(news.getRecruitmentStatus()),
                news.getTargetAudience(),
                news.getContact(),
                news.getManager(),
                news.getPublishedAt().toLocalDate(),
                news.getProgramStartDate(),
                news.getProgramEndDate(),
                news.getApplyStartDate(),
                news.getApplyEndDate(),
                news.getViewCount(),
                news.getScrapCount(),
                scrapped
        );
    }
}
