package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.LocalWelfareApiResponseDto;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.service.InfoTagMappingService;
import com.bodeum.domain.info.util.RegionMapper;
import com.bodeum.global.infrastructure.openapi.publicDataApi.LocalWelfareApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalWelfareSyncService {

    private static final Long LOCAL_WELFARE_CATEGORY_ID = 13L;

    private final LocalWelfareApiClient localWelfareApiClient;
    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final RegionMapper regionMapper;
    private final InfoTagMappingService infoTagMappingService;

    @Transactional
    public void syncLocalWelfareData() {
        log.info("[지자체 복지 서비스 API 동기화] 수집 및 DB 저장 시작");

        InfoCategory category = infoCategoryRepository.findById(LOCAL_WELFARE_CATEGORY_ID)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_CATEGORY_NOT_FOUND));

        List<LocalWelfareApiResponseDto.ServList> apiDataList = localWelfareApiClient.fetchAllData();
        LocalDateTime now = LocalDateTime.now();

        int insertedCount = 0;
        int updatedCount = 0;

        for (LocalWelfareApiResponseDto.ServList item : apiDataList) {
            String servNm = item.servNm();
            if (servNm == null || servNm.isBlank()) {
                continue;
            }

            String externalId = item.toExternalId();
            String displayName = servNm.trim();

            String sido = parseSido(item.ctpvNm());
            String sigungu = item.sggNm() != null ? item.sggNm().trim() : "";
            Long regionId = regionMapper.resolveRegionId(sido, sigungu);

            StringBuilder introBuilder = new StringBuilder();
            if (item.servDgst() != null && !item.servDgst().isBlank()) {
                introBuilder.append("■ 서비스요약: ").append(item.servDgst().trim()).append("\n");
            }
            if (item.bizChrDeptNm() != null && !item.bizChrDeptNm().isBlank()) {
                introBuilder.append("■ 담당부서: ").append(item.bizChrDeptNm().trim()).append("\n");
            }
            if (item.aplyMtdNm() != null && !item.aplyMtdNm().isBlank()) {
                introBuilder.append("■ 신청방법: ").append(item.aplyMtdNm().trim()).append("\n");
            }
            if (item.trgterIndvdlNmArray() != null && !item.trgterIndvdlNmArray().isBlank()) {
                introBuilder.append("■ 지원대상: ").append(item.trgterIndvdlNmArray().trim());
            }

            String introduction = introBuilder.toString().trim();
            String homepageUrl = item.servDtlLink();

            InfoItem infoItem = infoItemRepository.findFirstByExternalId(externalId)
                    .map(existingItem -> {
                        existingItem.updateInformation(
                                displayName, category, regionId, introduction, null,
                                sido, sigungu, null, homepageUrl, null
                        );
                        return existingItem;
                    })
                    .orElseGet(() -> InfoItem.builder()
                            .externalId(externalId)
                            .infoCategory(category)
                            .regionId(regionId)
                            .name(displayName)
                            .introduction(introduction)
                            .sido(sido)
                            .sigungu(sigungu)
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

        log.info("[지자체 복지 서비스 API 동기화] 완료 - 신규: {}건, 수정: {}건", insertedCount, updatedCount);
    }

    private String parseSido(String rawSido) {
        if (rawSido == null || rawSido.isBlank()) return "";
        String token = rawSido.trim();
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
}