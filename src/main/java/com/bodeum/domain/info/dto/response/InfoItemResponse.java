package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;

public record InfoItemResponse(
        Long infoItemId,
        String name,
        MainCategory mainCategory,
        String mainCategoryKo,
        Long subCategoryId,
        String subCategory,      // 예: "EMERGENCY_MEDICAL_CENTER" 또는 영문명
        String subCategoryKo,    // 예: "응급의료기관"
        String address,
        String sido,
        String sigungu,
        String phone,
        String homepageUrl,
        int viewCount,
        int scrapCount,
        int reviewCount
) {
    public static InfoItemResponse from(InfoItem entity) {
        return new InfoItemResponse(
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
                entity.getReviewCount()
        );
    }
}