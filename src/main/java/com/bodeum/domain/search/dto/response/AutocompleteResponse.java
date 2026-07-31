package com.bodeum.domain.search.dto.response;

import com.bodeum.domain.info.entity.InfoItem;

import java.util.List;

public record AutocompleteResponse(
        List<AutocompleteItemDto> items
) {
    public record AutocompleteItemDto(
            Long infoItemId,
            String name,
            String category,
            String categoryLabel,
            String regionLevel1,
            String regionLevel2,
            List<String> tags
    ) {
        public static AutocompleteItemDto from(InfoItem item) {
            return new AutocompleteItemDto(
                    item.getId(),
                    item.getName(),
                    item.getInfoCategory().getMainCategory().name(),
                    item.getInfoCategory().getMainCategoryKo(), // Entity 내 한글 명칭 활용
                    item.getSido(),
                    item.getSigungu(),
                    item.getTags() // Entity 내 편의 메서드 활용
            );
        }
    }

    public static AutocompleteResponse from(List<InfoItem> items) {
        List<AutocompleteItemDto> dtos = items.stream()
                .map(AutocompleteItemDto::from)
                .toList();
        return new AutocompleteResponse(dtos);
    }
}