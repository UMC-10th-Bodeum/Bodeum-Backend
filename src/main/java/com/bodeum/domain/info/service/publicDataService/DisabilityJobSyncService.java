package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.DisabilityJobApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.DisabilityJobApiClient;
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
public class DisabilityJobSyncService {

    private static final Long DISABILITY_JOB_CATEGORY_ID = 19L;

    private final DisabilityJobApiClient disabilityJobApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] syncDisabilityJobData() {
        log.info("[장애인 구인 정보 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(DISABILITY_JOB_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<DisabilityJobApiResponseDto.HeaderData> apiDataList = disabilityJobApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (DisabilityJobApiResponseDto.HeaderData item : apiDataList) {
            String companyName = item.workplaceName();
            String jobTitle = item.recruitmentJob();

            if (companyName == null || companyName.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String displayName = (jobTitle != null && !jobTitle.isBlank())
                    ? "[" + companyName.trim() + "] " + jobTitle.trim()
                    : companyName.trim();

            String rawAddress = item.address() != null ? item.address().trim() : "";
            String sido = parseSido(rawAddress);
            String sigungu = parseSigungu(rawAddress, sido);

            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            StringBuilder introBuilder = new StringBuilder();
            if (jobTitle != null) {
                introBuilder.append("■ 모집직종: ").append(jobTitle).append("\n");
            }
            if (item.employmentType() != null) {
                introBuilder.append("■ 고용형태: ").append(item.employmentType()).append("\n");
            }
            if (item.wage() != null) {
                String wageTypeStr = item.wageType() != null ? item.wageType() : "";
                introBuilder.append("■ 임금조건: ").append(wageTypeStr).append(" ").append(String.valueOf(item.wage())).append("원\n");
            }
            if (item.recruitmentPeriod() != null) {
                introBuilder.append("■ 모집기간: ").append(item.recruitmentPeriod()).append("\n");
            }
            if (item.requiredExperience() != null) {
                introBuilder.append("■ 요구경력: ").append(item.requiredExperience()).append("\n");
            }
            if (item.requiredEducation() != null) {
                introBuilder.append("■ 요구학력: ").append(item.requiredEducation()).append("\n");
            }
            if (item.companyType() != null) {
                introBuilder.append("■ 기업형태: ").append(item.companyType()).append("\n");
            }
            if (item.agency() != null) {
                introBuilder.append("■ 담당기관: ").append(item.agency());
            }

            String rawIntro = introBuilder.toString().trim();
            String introduction = rawIntro.length() > 2000 ? rawIntro.substring(0, 2000) : rawIntro;
            String phone = item.contact();

            Optional<InfoItem> existingOpt = infoItemRepository.findByExternalId(externalId);

            InfoItem savedItem;
            if (existingOpt.isPresent()) {
                savedItem = existingOpt.get();
                savedItem.updateInformation(
                        displayName, category, regionId, introduction, rawAddress,
                        sido, sigungu, phone, null, null
                );
                updatedCount++;
            } else {
                InfoItem newItem = InfoItem.builder()
                        .externalId(externalId)
                        .infoCategory(category)
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
                        .build();

                savedItem = infoItemRepository.save(newItem);
                insertedCount++;
            }

            // ★ 태그 자동 매핑 실행
            infoTagMappingService.autoMapTags(savedItem);
        }

        entityManager.flush();

        log.info("[장애인 구인 정보 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
        return new int[]{insertedCount, updatedCount};
    }

    private String parseSido(String address) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(" ");
        if (parts.length > 0) {
            String token = parts[0];
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
            if (token.startsWith("전북") || token.startsWith("전라북")) return "전라북도";
            if (token.startsWith("전남") || token.startsWith("전라남")) return "전라남도";
            if (token.startsWith("경북") || token.startsWith("경상북")) return "경상북도";
            if (token.startsWith("경남") || token.startsWith("경상남")) return "경상남도";
            if (token.startsWith("제주")) return "제주특별자치도";
            return token;
        }
        return "";
    }

    private String parseSigungu(String address, String sido) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(" ");
        if (parts.length > 1) {
            return parts[1];
        }
        return "";
    }
}