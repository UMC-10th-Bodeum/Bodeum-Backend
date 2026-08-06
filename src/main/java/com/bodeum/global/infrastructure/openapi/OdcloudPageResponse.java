package com.bodeum.global.infrastructure.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdcloudPageResponse(
        int page,
        int perPage,
        int totalCount,
        int currentCount,
        List<Map<String, Object>> data
) {

    public OdcloudPageResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
