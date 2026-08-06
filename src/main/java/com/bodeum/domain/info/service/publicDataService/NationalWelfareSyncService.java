package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.NationalWelfareApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.global.infrastructure.openapi.publicDataApi.NationalWelfareApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NationalWelfareSyncService {

    private static final Long NATIONAL_WELFARE_CATEGORY_ID = 12L;

    private final NationalWelfareApiClient nationalWelfareApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncNationalWelfareData() {
        log.info("[중앙부처 복지 서비스 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(NATIONAL_WELFARE_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<NationalWelfareApiResponseDto.ServList> apiDataList = nationalWelfareApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (NationalWelfareApiResponseDto.ServList item : apiDataList) {
            String servNm = item.servNm();
            if (servNm == null || servNm.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String displayName = servNm.trim();

            StringBuilder introBuilder = new StringBuilder();
            if (item.servDgst() != null && !item.servDgst().isBlank()) {
                introBuilder.append("■ 서비스요약: ").append(item.servDgst().trim()).append("\n");
            }
            if (item.jurMnofNm() != null && !item.jurMnofNm().isBlank()) {
                introBuilder.append("■ 소관부처: ").append(item.jurMnofNm().trim()).append("\n");
            }
            if (item.jurOrgNm() != null && !item.jurOrgNm().isBlank()) {
                introBuilder.append("■ 담당기관: ").append(item.jurOrgNm().trim()).append("\n");
            }
            if (item.trgterIndvdlArray() != null && !item.trgterIndvdlArray().isBlank()) {
                introBuilder.append("■ 대상자: ").append(item.trgterIndvdlArray().trim());
            }

            String introduction = introBuilder.toString().trim();
            String phone = item.rprsCtadr();
            String homepageUrl = item.servDtlLink();

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                displayName, category, null, introduction, null,
                                null, null, phone, homepageUrl, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(null)
                            .name(displayName)
                            .introduction(introduction)
                            .phone(phone)
                            .homepageUrl(homepageUrl)
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

        log.info("[중앙부처 복지 서비스 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }
}