package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.PrivateWelfareApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.global.infrastructure.openapi.publicDataApi.PrivateWelfareApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateWelfareSyncService {

    private static final Long PRIVATE_WELFARE_CATEGORY_ID = 11L;

    private final PrivateWelfareApiClient privateWelfareApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncPrivateWelfareData() {
        log.info("[민간 복지 서비스 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(PRIVATE_WELFARE_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<PrivateWelfareApiResponseDto.HeaderData> apiDataList = privateWelfareApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (PrivateWelfareApiResponseDto.HeaderData item : apiDataList) {
            String businessName = item.businessName();
            String orgName = item.organizationName();

            if (businessName == null || businessName.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String displayName = (orgName != null && !orgName.isBlank())
                    ? "[" + orgName.trim() + "] " + businessName.trim()
                    : businessName.trim();

            StringBuilder introBuilder = new StringBuilder();
            if (item.businessPurpose() != null && !item.businessPurpose().isBlank()) {
                introBuilder.append("■ 사업목적: ").append(item.businessPurpose().trim()).append("\n");
            }
            if (item.supportContent() != null && !item.supportContent().isBlank()) {
                introBuilder.append("■ 지원내용: ").append(item.supportContent().trim()).append("\n");
            }
            if (item.supportTarget() != null && !item.supportTarget().isBlank()) {
                introBuilder.append("■ 지원대상: ").append(item.supportTarget().trim()).append("\n");
            }
            if (item.applicationMethod() != null && !item.applicationMethod().isBlank()) {
                introBuilder.append("■ 신청방법: ").append(item.applicationMethod().trim()).append("\n");
            }
            if (item.requiredDocuments() != null && !item.requiredDocuments().isBlank()) {
                introBuilder.append("■ 제출서류: ").append(item.requiredDocuments().trim());
            }

            String introduction = introBuilder.toString().trim();

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                displayName, category, InterestCategory.WELFARE_SUBSIDY, null,introduction, null,
                                null, null, null, null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .interest(InterestCategory.WELFARE_SUBSIDY)
                            .regionId(null)
                            .name(displayName)
                            .introduction(introduction)
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

        log.info("[민간 복지 서비스 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }
}