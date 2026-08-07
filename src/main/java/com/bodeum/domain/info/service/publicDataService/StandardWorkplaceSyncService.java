package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.StandardWorkplaceApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.global.infrastructure.openapi.publicDataApi.StandardWorkplaceApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardWorkplaceSyncService {

    private static final Long STANDARD_WORKPLACE_CATEGORY_ID = 21L;

    private final StandardWorkplaceApiClient standardWorkplaceApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncStandardWorkplaceData() {
        log.info("[장애인 표준사업장 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(STANDARD_WORKPLACE_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<StandardWorkplaceApiResponseDto.HeaderData> apiDataList = standardWorkplaceApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (StandardWorkplaceApiResponseDto.HeaderData item : apiDataList) {
            String companyName = item.companyName();
            if (companyName == null || companyName.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();

            String typePrefix = item.categoryType() != null && !item.categoryType().isBlank()
                    ? "[" + item.categoryType().trim() + "] "
                    : "";
            String displayName = typePrefix + companyName.trim();

            String rawAddress = item.address() != null ? item.address().trim() : "";
            String sido = parseSido(rawAddress);
            String sigungu = parseSigungu(rawAddress);

            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            StringBuilder introBuilder = new StringBuilder();
            if (item.businessTypeAndProducts() != null && !item.businessTypeAndProducts().isBlank()) {
                introBuilder.append("■ 업종 및 주요생산품: ").append(item.businessTypeAndProducts().trim()).append("\n");
            }
            if (item.ceoName() != null && !item.ceoName().isBlank()) {
                introBuilder.append("■ 대표자: ").append(item.ceoName().trim()).append("\n");
            }
            if (item.categoryType() != null && !item.categoryType().isBlank()) {
                introBuilder.append("■ 구분: ").append(item.categoryType().trim()).append("\n");
            }
            if (item.certNumber() != null && !item.certNumber().isBlank()) {
                introBuilder.append("■ 인증번호: ").append(item.certNumber().trim()).append("\n");
            }
            if (item.certDate() != null && !item.certDate().isBlank()) {
                introBuilder.append("■ 인증일자: ").append(item.certDate().trim()).append("\n");
            }
            if (item.agency() != null && !item.agency().isBlank()) {
                introBuilder.append("■ 관할지사: ").append(item.agency().trim());
            }

            String introduction = introBuilder.toString().trim();
            String phone = item.phone();

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                displayName, category, InterestCategory.WELFARE_SUBSIDY,regionId, introduction, rawAddress,
                                sido, sigungu, phone, null, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .interest(InterestCategory.WELFARE_SUBSIDY)
                            .regionId(regionId)
                            .name(displayName)
                            .introduction(introduction)
                            .address(rawAddress)
                            .sido(sido)
                            .sigungu(sigungu)
                            .phone(phone)
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

        log.info("[장애인 표준사업장 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String parseSido(String address) {
        if (address == null || address.isBlank()) return "";
        String token = address.split(" ")[0];
        if (token.startsWith("서울")) return "서울특별시";
        if (token.startsWith("경기")) return "경기도";
        if (token.startsWith("인천")) return "인천광역시";
        if (token.startsWith("부산")) return "부산광역시";
        if (token.startsWith("대구")) return "대구광역시";
        if (token.startsWith("광주")) return "광주광역시";
        if (token.startsWith("대전")) return "대전광역시";
        if (token.startsWith("울산")) return "울산광역시";
        if (token.startsWith("세종")) return "세종특별자치시";
        if (token.startsWith("강원")) return "강원특별자치도";
        if (token.startsWith("충북") || token.startsWith("충청북")) return "충청북도";
        if (token.startsWith("충남") || token.startsWith("충청남")) return "충청남도";
        if (token.startsWith("전북") || token.startsWith("전라북")) return "전북특별자치도";
        if (token.startsWith("전남") || token.startsWith("전라남")) return "전라남도";
        if (token.startsWith("경북") || token.startsWith("경상북")) return "경상북도";
        if (token.startsWith("경남") || token.startsWith("경상남")) return "경상남도";
        if (token.startsWith("제주")) return "제주특별자치도";
        return token;
    }

    private String parseSigungu(String address) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(" ");
        return parts.length > 1 ? parts[1] : "";
    }
}