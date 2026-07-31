package com.bodeum.global.infrastructure.openapi;

import java.util.List;
import java.util.Map;

public record GgOpenApiPageResponse(
        int totalCount,
        List<Map<String, Object>> rows
) {

    public GgOpenApiPageResponse {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
