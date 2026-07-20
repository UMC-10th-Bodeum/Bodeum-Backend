package com.bodeum.domain.search.dto.response;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;

import java.util.List;

public record InfoSearchResponse(
        List<InfoItemDto> results,
        long totalCount
) {
    public record InfoItemDto(
            Long infoItemId,
            String category,
            String categoryLabel,
            String name,
            String introduction,
            String address,
            String sido,
            String sigungu,
            String phone,
            String homepageUrl,
            int viewCount,
            int scrapCount,
            int reviewCount
    ) {
        public static InfoItemDto from(InfoItem item) {
            return new InfoItemDto(
                    item.getId(),
                    item.getInfoCategory().getMainCategory().name(),
                    toCategoryLabel(item.getInfoCategory().getMainCategory()),
                    item.getName(),
                    item.getIntroduction(),
                    item.getAddress(),
                    item.getSido(),
                    item.getSigungu(),
                    item.getPhone(),
                    item.getHomepageUrl(),
                    item.getViewCount(),
                    item.getScrapCount(),
                    item.getReviewCount()
            );
        }

        private static String toCategoryLabel(MainCategory category) {
            return switch (category) {
                case INSTITUTION -> "기관";
                case HOSPITAL -> "병원";
                case WELFARE -> "복지";
                case EMPLOYMENT -> "취업";
                case EDUCATION -> "교육";
            };
        }
    }

    public static InfoSearchResponse of(List<InfoItem> items) {
        List<InfoItemDto> results = items.stream().map(InfoItemDto::from).toList();
        return new InfoSearchResponse(results, results.size());
    }
}
