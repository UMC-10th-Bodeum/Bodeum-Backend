package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.enums.MainCategory;
import org.springframework.data.domain.Page;

public record InfoItemPageResponse(
        MainCategory selectedMainCategory,
        String selectedMainCategoryKo,
        Long selectedSubCategoryId,
        String selectedSubCategory,    // 추가!
        String selectedSubCategoryKo,
        Page<InfoItemResponse> items
) {
    public static InfoItemPageResponse of(
            MainCategory mainCategory,
            String mainCategoryKo,
            Long subCategoryId,
            String subCategory,
            String subCategoryKo,
            Page<InfoItemResponse> items
    ) {
        return new InfoItemPageResponse(
                mainCategory,
                mainCategoryKo,
                subCategoryId,
                subCategory,
                subCategoryKo,
                items
        );
    }
}