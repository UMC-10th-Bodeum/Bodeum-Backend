package com.bodeum.global.infrastructure.openapi;

import java.util.List;
import java.util.Map;

public record DataGoOpenApiPageResponse(
        int totalCount,
        List<Map<String, Object>> rows
) {
}
