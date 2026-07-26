package com.bodeum.domain.news.dto.response;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NewsDetailResponse(
        @Schema(description = "소식 ID", example = "1")
        Long newsId,
        String title,
        String summary,
        String content,
        @Schema(description = "지역", example = "경기도 수원시")
        String region,
        @Schema(description = "카테고리", example = "SUPPORT_SERVICE")
        String category,
        String sourceName,
        String originalUrl,
        String thumbnailUrl,
        NewsType newsType,
        @Schema(description = "모집 상태", example = "RECRUITING")
        NewsStatus status,
        String targetAudience,
        String contact,
        String manager,
        LocalDateTime publishedAt,
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
                news.getNewsCategory().getName(),
                news.getSourceName(),
                news.getOriginalUrl(),
                news.getThumbnailUrl(),
                news.getNewsType(),
                NewsStatus.from(news.getRecruitmentStatus()),
                news.getTargetAudience(),
                news.getContact(),
                news.getManager(),
                news.getPublishedAt(),
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
