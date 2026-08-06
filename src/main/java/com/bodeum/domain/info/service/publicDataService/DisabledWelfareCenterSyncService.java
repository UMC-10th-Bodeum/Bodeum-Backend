package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.DisabledWelfareCenterApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.global.infrastructure.openapi.publicDataApi.DisabledWelfareCenterApiClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisabledWelfareCenterSyncService {

    private static final Long WELFARE_CENTER_CATEGORY_ID = 7L;

    private final DisabledWelfareCenterApiClient disabledWelfareCenterApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] syncDisabledWelfareCenterData() {
        log.info("[장애인 복지관 API 동기화] 데이터 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(WELFARE_CENTER_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<DisabledWelfareCenterApiResponseDto.HeaderData> apiDataList = disabledWelfareCenterApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (DisabledWelfareCenterApiResponseDto.HeaderData item : apiDataList) {
            String name = item.name();
            String address = item.address();

            if (name == null || address == null || name.isBlank() || address.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String[] parsedAddress = parseAddress(address);
            String sido = (item.sido() != null && !item.sido().isBlank()) ? item.sido() : parsedAddress[0];
            String sigungu = (item.sigungu() != null && !item.sigungu().isBlank()) ? item.sigungu() : parsedAddress[1];
            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            String phone = item.phone();

            StringBuilder introBuilder = new StringBuilder();
            if (item.facilityType() != null && !item.facilityType().isBlank()) {
                introBuilder.append("■ 시설유형: ").append(item.facilityType()).append("\n");
            }
            if (item.corporateStatus() != null && !item.corporateStatus().isBlank()) {
                introBuilder.append("■ 법인현황: ").append(item.corporateStatus()).append("\n");
            }
            if (item.fax() != null && !item.fax().isBlank()) {
                introBuilder.append("■ 팩스번호: ").append(item.fax());
            }
            String introduction = introBuilder.toString().trim();

            Optional<InfoItem> existingOpt = infoItemRepository.findFirstByExternalId(externalId);

            InfoItem savedItem;
            if (existingOpt.isPresent()) {
                savedItem = existingOpt.get();
                savedItem.updateInformation(
                        name, category, InterestCategory.PARENTING_COMMUNICATION, regionId, introduction, address,
                        sido, sigungu, phone, null, null
                );
                updatedCount++;
            } else {
                InfoItem newItem = InfoItem.builder()
                        .externalId(externalId)
                        .infoCategory(category)
                        .regionId(regionId)
                        .name(name)
                        .introduction(introduction)
                        .address(address)
                        .sido(sido)
                        .sigungu(sigungu)
                        .phone(phone)
                        .homepageUrl(null)
                        .syncedAt(now)
                        .build();

                savedItem = infoItemRepository.save(newItem);
                insertedCount++;
            }

            // ★ 태그 자동 매핑 실행
            infoTagMappingService.autoMapTags(savedItem);
        }

        entityManager.flush();
        log.info("[장애인 복지관 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
        return new int[]{insertedCount, updatedCount};
    }

    private String[] parseAddress(String fullAddress) {
        String[] tokens = fullAddress.trim().split("\\s+");
        String sido = tokens.length > 0 ? tokens[0] : "";
        String sigungu = tokens.length > 1 ? tokens[1] : "";
        return new String[]{sido, sigungu};
    }
}