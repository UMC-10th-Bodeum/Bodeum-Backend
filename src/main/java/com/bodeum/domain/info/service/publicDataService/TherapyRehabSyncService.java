package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.TherapyRehabApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.TherapyRehabApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TherapyRehabSyncService {

    private static final Long THERAPY_REHAB_CATEGORY_ID = 6L;

    private final TherapyRehabApiClient therapyRehabApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncTherapyRehabData() {
        log.info("[치료·재활 API 동기화] 데이터 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(THERAPY_REHAB_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<TherapyRehabApiResponseDto.HeaderData> apiDataList = therapyRehabApiClient.fetchAllTherapyRehabData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (TherapyRehabApiResponseDto.HeaderData item : apiDataList) {
            String name = item.name();
            String address = item.address();

            if (name == null || address == null || name.isBlank() || address.isBlank()) {
                continue;
            }

            String externalId = "THERAPY_" + name.trim() + "_" + address.trim();

            String[] parsedAddress = parseAddress(address, item.sigungu());
            String sido = parsedAddress[0];
            String sigungu = parsedAddress[1];

            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                name, category, regionId, null, address,
                                sido, sigungu, null, null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(regionId)
                            .name(name)
                            .address(address)
                            .sido(sido)
                            .sigungu(sigungu)
                            .syncedAt(now)
                            .build());

            if (infoItem.getId() == null) {
                infoItemRepository.save(infoItem);
                insertedCount++;
            } else {
                updatedCount++;
            }

            // ★ 태그 자동 매핑
            infoTagMappingService.autoMapTags(infoItem);
        }

        log.info("[치료·재활 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String[] parseAddress(String fullAddress, String sigunguParam) {
        String[] tokens = fullAddress.trim().split("\\s+");
        String rawSido = tokens.length > 0 ? tokens[0] : "";
        String sigungu = tokens.length > 1 ? tokens[1] : (sigunguParam != null ? sigunguParam : "");

        String sido = rawSido;
        if (rawSido.startsWith("서울")) sido = "서울특별시";
        else if (rawSido.startsWith("경기")) sido = "경기도";
        else if (rawSido.startsWith("인천")) sido = "인천광역시";
        else if (rawSido.startsWith("부산")) sido = "부산광역시";
        else if (rawSido.startsWith("대구")) sido = "대구광역시";
        else if (rawSido.startsWith("광주")) sido = "광주광역시";
        else if (rawSido.startsWith("대전")) sido = "대전광역시";
        else if (rawSido.startsWith("울산")) sido = "울산광역시";
        else if (rawSido.startsWith("세종")) sido = "세종특별자치시";
        else if (rawSido.startsWith("강원")) sido = "강원특별자치도";
        else if (rawSido.startsWith("충북") || rawSido.startsWith("충청북")) sido = "충청북도";
        else if (rawSido.startsWith("충남") || rawSido.startsWith("충청남")) sido = "충청남도";
        else if (rawSido.startsWith("전북") || rawSido.startsWith("전라북")) sido = "전북특별자치도";
        else if (rawSido.startsWith("전남") || rawSido.startsWith("전라남")) sido = "전라남도";
        else if (rawSido.startsWith("경북") || rawSido.startsWith("경상북")) sido = "경상북도";
        else if (rawSido.startsWith("경남") || rawSido.startsWith("경상남")) sido = "경상남도";
        else if (rawSido.startsWith("제주")) sido = "제주특별자치도";

        return new String[]{sido, sigungu};
    }
}