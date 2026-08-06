package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.PublicDoctorApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.global.infrastructure.openapi.publicDataApi.PublicDoctorApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDoctorSyncService {

    private static final Long PRIMARY_CARE_CATEGORY_ID = 3L;

    private final PublicDoctorApiClient publicDoctorApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncPublicDoctorData() {
        log.info("[장애인 건강주치의 API 동기화] 데이터 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(PRIMARY_CARE_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<PublicDoctorApiResponseDto.HeaderData> apiDataList = publicDoctorApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (PublicDoctorApiResponseDto.HeaderData item : apiDataList) {
            String name = item.name();
            String address = item.address();

            if (name == null || address == null || name.isBlank() || address.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();

            String[] parsedAddress = parseAddress(address);
            String sido = parsedAddress[0];
            String sigungu = parsedAddress[1];
            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            String introduction = String.format("구분: %s | 서비스유형: %s",
                    item.category() != null ? item.category() : "-",
                    item.serviceType() != null ? item.serviceType() : "-");

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                name, category, InterestCategory.HOSPITAL_HEALTH,regionId, introduction, address,
                                sido, sigungu, null, null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .interest(InterestCategory.HOSPITAL_HEALTH)
                            .regionId(regionId)
                            .name(name)
                            .introduction(introduction)
                            .address(address)
                            .sido(sido)
                            .sigungu(sigungu)
                            .phone(null)
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

            // ★ 태그 자동 매핑
            infoTagMappingService.autoMapTags(infoItem);
        }

        log.info("[장애인 건강주치의 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String[] parseAddress(String fullAddress) {
        String[] tokens = fullAddress.trim().split("\\s+");
        String sido = tokens.length > 0 ? tokens[0] : "";
        String sigungu = tokens.length > 1 ? tokens[1] : "";

        return new String[]{sido, sigungu};
    }
}