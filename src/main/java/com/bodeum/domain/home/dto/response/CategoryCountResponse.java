package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.info.entity.enums.MainCategory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CategoryCountResponse(
        long institution,
        long hospital,
        long welfare,
        long employment,
        long education
) {
    public static CategoryCountResponse from(List<Object[]> rows) {
        Map<MainCategory, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                        row -> (MainCategory) row[0],
                        row -> (Long) row[1]
                ));
        return new CategoryCountResponse(
                counts.getOrDefault(MainCategory.INSTITUTION, 0L),
                counts.getOrDefault(MainCategory.HOSPITAL, 0L),
                counts.getOrDefault(MainCategory.WELFARE, 0L),
                counts.getOrDefault(MainCategory.EMPLOYMENT, 0L),
                counts.getOrDefault(MainCategory.EDUCATION, 0L)
        );
    }
}
