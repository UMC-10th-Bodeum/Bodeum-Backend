package com.bodeum.domain.region.service;

import java.util.List;

public record ResolvedRegionFilter(
        boolean applied,
        List<Long> regionIds
) {

    public ResolvedRegionFilter {
        regionIds = List.copyOf(regionIds);
    }

    public static ResolvedRegionFilter none() {
        return new ResolvedRegionFilter(false, List.of());
    }

    public static ResolvedRegionFilter applied(List<Long> regionIds) {
        return new ResolvedRegionFilter(true, regionIds);
    }
}
