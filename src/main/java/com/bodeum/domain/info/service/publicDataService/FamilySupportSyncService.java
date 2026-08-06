package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.FamilySupportApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.FamilySupportApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilySupportSyncService {

    private static final Long FAMILY_SUPPORT_CATEGORY_ID = 9L;

    private final FamilySupportApiClient familySupportApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncFamilySupportData() {
        log.info("[장애인가족지원센터 API 동기화] 데이터 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(FAMILY_SUPPORT_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<FamilySupportApiResponseDto.HeaderData> apiDataList = familySupportApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (FamilySupportApiResponseDto.HeaderData item : apiDataList) {
            String name = item.name();
            String fullAddress = item.getFullAddress();

            if (name == null || fullAddress == null || name.isBlank() || fullAddress.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();

            String[] parsedAddress = parseAddress(fullAddress, item.sido(), item.sigungu());
            String sido = parsedAddress[0];
            String sigungu = parsedAddress[1];
            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            String introduction = String.format("사업유형: %s | 대표자: %s",
                    item.businessType() != null ? item.businessType() : "-",
                    item.representative() != null ? item.representative() : "-");

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                name, category, regionId, introduction, fullAddress,
                                sido, sigungu, item.phone(), null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(regionId)
                            .name(name)
                            .introduction(introduction)
                            .address(fullAddress)
                            .sido(sido)
                            .sigungu(sigungu)
                            .phone(item.phone())
                            .homepageUrl(null)
                            .imageUrl(null)
                            .syncedAt(now)
                            .build());

            if (infoItem.getId() == null) {
                infoItemRepository.save(infoItem);
                insertedCount++;
            } else {
                updatedCount++;
            }

            // ★ 태그 자동 매핑 실행
            infoTagMappingService.autoMapTags(infoItem);
        }

        log.info("[장애인가족지원센터 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String[] parseAddress(String fullAddress, String sidoParam, String sigunguParam) {
        String[] tokens = fullAddress.trim().split("\\s+");
        String sido = (sidoParam != null && !sidoParam.isBlank()) ? sidoParam : (tokens.length > 0 ? tokens[0] : "");

        String sigungu = sigunguParam;
        if (sigungu != null && sigungu.contains(" ")) {
            String[] parts = sigungu.split("\\s+");
            sigungu = parts[parts.length - 1];
        } else if (sigungu == null || sigungu.isBlank()) {
            sigungu = (tokens.length > 1) ? tokens[1] : "";
        }

        return new String[]{sido, sigungu};
    }
}