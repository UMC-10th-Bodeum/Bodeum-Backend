package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;

import java.util.List;

public record InfoItemResponse(
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
        List<String> tags // ★ 태그 목록 추가
) {
    public static InfoItemResponse of(InfoItem entity, List<String> tags) {
        InfoCategory category = entity.getInfoCategory();

        return new InfoItemResponse(
                entity.getId(),
                entity.getName(),
                category != null ? category.getMainCategory() : null,
                category != null ? category.getMainCategoryKo() : null,
                category != null ? category.getId() : null,
                category != null ? category.getSubCategory() : null,
                category != null ? category.getSubCategoryKo() : null,
                entity.getAddress(),
                entity.getSido(),
                entity.getSigungu(),
                entity.getPhone(),
                entity.getHomepageUrl(),
                entity.getViewCount(),
                entity.getScrapCount(),
                entity.getReviewCount(),
                tags != null ? tags : List.of()
        );
    }
}