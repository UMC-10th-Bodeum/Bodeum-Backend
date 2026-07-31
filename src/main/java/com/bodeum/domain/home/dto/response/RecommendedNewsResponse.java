package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.region.entity.Region;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record RecommendedNewsResponse(
        Long newsId,
        String title,
        String thumbnailUrl,
        String regionLevel1,
        String regionLevel2,
        String dDay,
        String status,
        Long viewCount
) {
    public static RecommendedNewsResponse from(News news, Region region) {
        return new RecommendedNewsResponse(
                news.getId(),
                news.getTitle(),
                news.getThumbnailUrl(),
                region != null ? region.getRegionLevel1() : null,
                region != null ? region.getRegionLevel2() : null,
                calculateDDay(news.getApplyEndDate()),
                toStatusLabel(news.getRecruitmentStatus()),
                news.getViewCount()
        );
    }

    private static String calculateDDay(LocalDate applyEndDate) {
        if (applyEndDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), applyEndDate);
        if (days < 0) {
            return "마감";
        }
        if (days == 0) {
            return "D-Day";
        }
        return "D-" + days;
    }

    private static String toStatusLabel(RecruitmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OPEN -> "모집중";
            case CLOSED -> "모집 마감";
            case ALWAYS_OPEN -> "상시 모집";
            case UPCOMING -> "모집 예정";
        };
    }
}
