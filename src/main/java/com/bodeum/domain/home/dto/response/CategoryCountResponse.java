package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.info.entity.enums.InfoCategory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CategoryCountResponse(
        long institution,
        long hospital,
        long welfare,
        long job,
        long education
) {
    public static CategoryCountResponse from(List<Object[]> rows) {
        Map<InfoCategory, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                        row -> (InfoCategory) row[0],
                        row -> (Long) row[1]
                ));
        return new CategoryCountResponse(
                counts.getOrDefault(InfoCategory.INSTITUTION, 0L),
                counts.getOrDefault(InfoCategory.HOSPITAL, 0L),
                counts.getOrDefault(InfoCategory.WELFARE, 0L),
                counts.getOrDefault(InfoCategory.JOB, 0L),
                counts.getOrDefault(InfoCategory.EDUCATION, 0L)
        );
    }
}
