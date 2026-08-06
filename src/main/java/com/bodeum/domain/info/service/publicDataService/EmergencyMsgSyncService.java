package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.EmergencyMsgApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.EmergencyMsgApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyMsgSyncService {

    private static final Long EMERGENCY_MSG_CATEGORY_ID = 4L;

    private final EmergencyMsgApiClient emergencyMsgApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncEmergencyMessages() {
        log.info("[응급실 메시지 API 동기화] 전체 수집 시작");

        InfoCategory category = infoCategoryRepository.findById(EMERGENCY_MSG_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<EmergencyMsgApiResponseDto.Item> items = emergencyMsgApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (EmergencyMsgApiResponseDto.Item item : items) {
            String dutyName = item.dutyName();
            String address = item.dutyAddr();

            if (dutyName == null || address == null || dutyName.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();

            String[] parsedAddress = parseAddress(address);
            String sido = parsedAddress[0];
            String sigungu = parsedAddress[1];
            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            String introduction = String.format("[%s / %s] %s (구분: %s)",
                    item.trtPrtCodMag() != null ? item.trtPrtCodMag() : "전체",
                    item.symTypCodMag() != null ? item.symTypCodMag() : "응급실",
                    item.symBlkMsg() != null ? item.symBlkMsg() : "상세 메시지 없음",
                    item.symBlkMsgTyp() != null ? item.symBlkMsgTyp() : "일반"
            );

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                dutyName, category, regionId, introduction, address,
                                sido, sigungu, null, null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(regionId)
                            .name(dutyName)
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

            // ★ 태그 자동 매핑 실행
            infoTagMappingService.autoMapTags(infoItem);
        }

        log.info("[응급실 메시지 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    @Transactional
    public void syncEmergencyMessages(String stage1, String stage2) {
        syncEmergencyMessages();
    }

    private String[] parseAddress(String fullAddress) {
        String[] tokens = fullAddress.trim().split("\\s+");
        String sido = tokens.length > 0 ? tokens[0] : "";
        String sigungu = tokens.length > 1 ? tokens[1] : "";
        return new String[]{sido, sigungu};
    }
}