package com.bodeum.domain.home.dto.response;

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
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
        return new CategoryCountResponse(
                counts.getOrDefault("INSTITUTION", 0L),
                counts.getOrDefault("HOSPITAL", 0L),
                counts.getOrDefault("WELFARE", 0L),
                counts.getOrDefault("EMPLOYMENT", 0L),
                counts.getOrDefault("EDUCATION", 0L)
        );
    }
}
