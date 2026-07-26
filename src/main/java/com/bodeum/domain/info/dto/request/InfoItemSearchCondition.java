package com.bodeum.domain.info.dto.request;

import com.bodeum.domain.info.entity.enums.MainCategory;

public record InfoItemSearchCondition(
        MainCategory category,   // 대분류 (INSTITUTION|HOSPITAL|WELFARE|JOB|EDUCATION)
        Long subCategory,        // 카테고리별 소분류 ID
        String regionLevel1,     // 시/도
        String regionLevel2,     // 시/군/구
        String sort              // view|scrap|review (기본값: view)
) {
    // Compact Constructor를 활용한 비즈니스 기본값 처리 및 유저 프로필 지역 보완
    public InfoItemSearchCondition {
        // sort 파라미터 미입력 시 기본값 'view' 설정
        if (sort == null || sort.isBlank()) {
            sort = "view";
        }
    }

    // 로그인 유저의 온보딩 지역 정보가 필요할 때 새로운 레코드를 생성하는 편의 메서드
    public InfoItemSearchCondition withUserRegion(String defaultRegion1, String defaultRegion2) {
        return new InfoItemSearchCondition(
                this.category,
                this.subCategory,
                this.regionLevel1 != null ? this.regionLevel1 : defaultRegion1,
                this.regionLevel2 != null ? this.regionLevel2 : defaultRegion2,
                this.sort
        );
    }
}