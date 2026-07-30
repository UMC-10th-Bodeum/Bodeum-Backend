package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.SpecialSchoolApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.SpecialSchoolApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialSchoolSyncService {

    private static final Long SPECIAL_SCHOOL_CATEGORY_ID = 15L;

    private final SpecialSchoolApiClient specialSchoolApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;

    @Transactional
    public void syncSpecialSchoolData() {
        log.info("[특수학교 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(SPECIAL_SCHOOL_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<SpecialSchoolApiResponseDto.HeaderData> apiDataList = specialSchoolApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (SpecialSchoolApiResponseDto.HeaderData item : apiDataList) {
            String schoolName = item.schoolName();
            if (schoolName == null || schoolName.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String displayName = schoolName.trim();

            String rawAddress = item.address() != null ? item.address().trim() : "";
            String sido = parseSido(rawAddress, item.sidoName());
            String sigungu = parseSigungu(rawAddress);

            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            StringBuilder introBuilder = new StringBuilder();
            if (item.disabilityTarget() != null && !item.disabilityTarget().isBlank()) {
                introBuilder.append("■ 대상 장애영역: ").append(item.disabilityTarget().trim()).append("\n");
            }
            if (item.foundationType() != null && !item.foundationType().isBlank()) {
                introBuilder.append("■ 설립구분: ").append(item.foundationType().trim()).append("\n");
            }
            if (item.principalName() != null && !item.principalName().isBlank()) {
                introBuilder.append("■ 교장명: ").append(item.principalName().trim()).append("\n");
            }
            if (item.adminPhone() != null && !item.adminPhone().isBlank()) {
                introBuilder.append("■ 행정실: ").append(item.adminPhone().trim()).append("\n");
            }
            if (item.fax() != null && !item.fax().isBlank()) {
                introBuilder.append("■ 팩스번호: ").append(item.fax().trim()).append("\n");
            }
            if (item.openingDate() != null && !item.openingDate().isBlank()) {
                introBuilder.append("■ 개교년월일: ").append(item.openingDate().trim());
            }

            String introduction = introBuilder.toString().trim();
            String phone = item.getPrimaryPhone();
            String homepageUrl = item.getFormattedHomepageUrl();

            InfoItem infoItem = infoItemRepository.findByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                displayName, category, regionId, introduction, rawAddress,
                                sido, sigungu, phone, homepageUrl, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(regionId)
                            .name(displayName)
                            .introduction(introduction)
                            .address(rawAddress)
                            .sido(sido)
                            .sigungu(sigungu)
                            .phone(phone)
                            .homepageUrl(homepageUrl)
                            .imageUrl(null)
                            .syncedAt(now)
                            .build());

            if (infoItem.getId() == null) {
                infoItemRepository.save(infoItem);
                insertedCount++;
            } else {
                updatedCount++;
            }
        }

        log.info("[특수학교 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String parseSido(String address, String rawSido) {
        if (address != null && !address.isBlank()) {
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
        }

        if (rawSido != null) {
            if (rawSido.equals("서울")) return "서울특별시";
            if (rawSido.equals("경기")) return "경기도";
            if (rawSido.equals("인천")) return "인천광역시";
            if (rawSido.equals("부산")) return "부산광역시";
            if (rawSido.equals("대구")) return "대구광역시";
            if (rawSido.equals("광주")) return "광주광역시";
            if (rawSido.equals("대전")) return "대전광역시";
            if (rawSido.equals("울산")) return "울산광역시";
            if (rawSido.equals("전북")) return "전북특별자치도";
            if (rawSido.equals("전남")) return "전라남도";
            return rawSido;
        }
        return "";
    }

    private String parseSigungu(String address) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(" ");
        return parts.length > 1 ? parts[1] : "";
    }
}