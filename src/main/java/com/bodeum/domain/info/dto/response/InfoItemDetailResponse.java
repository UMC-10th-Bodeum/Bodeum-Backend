package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import java.util.List;

public record InfoItemDetailResponse(
        Long infoItemId,
        String name,
        MainCategory mainCategory,
        String mainCategoryKo,
        Long subCategoryId,
        String subCategory,
        String subCategoryKo,
        String address,
        String sido,
        String sigungu,
        String phone,
        String homepageUrl,
        int viewCount,
        int scrapCount,
        int reviewCount,
        boolean isScrapped,
        List<String> tags, // ★ 태그 목록 추가
        List<BusinessHourDto> businessHours
) {
    public static InfoItemDetailResponse of(
            InfoItem entity,
            boolean isScrapped,
            List<String> tags,
            List<BusinessHourDto> businessHours
    ) {
        return new InfoItemDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getInfoCategory().getMainCategory(),
                entity.getInfoCategory().getMainCategoryKo(),
                entity.getInfoCategory().getId(),
                entity.getInfoCategory().getSubCategory(),
                entity.getInfoCategory().getSubCategoryKo(),
                entity.getAddress(),
                entity.getSido(),
                entity.getSigungu(),
                entity.getPhone(),
                entity.getHomepageUrl(),
                entity.getViewCount(),
                entity.getScrapCount(),
                entity.getReviewCount(),
                isScrapped,
                tags != null ? tags : List.of(),
                businessHours
        );
    }

    public record BusinessHourDto(
            String dayOfWeek,
            String openTime,
            String closeTime
    ) {}
}