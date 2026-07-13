package com.bodeum.domain.region.dto.response;

import com.bodeum.domain.region.entity.Region;

public record RegionResponse(
        Long regionId,
        String regionLevel1,
        String regionLevel2,
        String fullName
) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getRegionLevel1(),
                region.getRegionLevel2(),
                region.getFullName()
        );
    }
}
