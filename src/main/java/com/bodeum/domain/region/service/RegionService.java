package com.bodeum.domain.region.service;

import com.bodeum.domain.region.dto.response.RegionResponse;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.exception.RegionErrorCode;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public List<RegionResponse> getRegions() {
        return regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc()
                .stream()
                .map(RegionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Region getById(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new ProjectException(RegionErrorCode.REGION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ResolvedRegionFilter resolveFilter(Long regionId, String regionLevel1) {
        if (regionId != null) {
            return ResolvedRegionFilter.applied(List.of(regionId));
        }
        if (!StringUtils.hasText(regionLevel1)) {
            return ResolvedRegionFilter.none();
        }
        return ResolvedRegionFilter.applied(
                regionRepository.findIdsByRegionLevel1(regionLevel1.trim())
        );
    }
}
