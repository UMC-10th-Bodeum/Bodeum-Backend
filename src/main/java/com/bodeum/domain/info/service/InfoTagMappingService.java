package com.bodeum.domain.info.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoItemTag;
import com.bodeum.domain.info.entity.InfoTag;
import com.bodeum.domain.info.repository.InfoItemTagRepository;
import com.bodeum.domain.info.repository.InfoTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InfoTagMappingService {

    private final InfoTagRepository infoTagRepository;
    private final InfoItemTagRepository infoItemTagRepository;

    /**
     * InfoItem 엔티티 수집/저장 시 자동 태그 매핑 처리
     */
    public void autoMapTags(InfoItem infoItem) {
        Set<String> tagNames = new HashSet<>();

        // 1. 카테고리 정보 기반 태그 추출
        if (infoItem.getInfoCategory() != null) {
            if (StringUtils.hasText(infoItem.getInfoCategory().getMainCategoryKo())) {
                tagNames.add(infoItem.getInfoCategory().getMainCategoryKo());
            }
            if (StringUtils.hasText(infoItem.getInfoCategory().getSubCategoryKo())) {
                tagNames.add(infoItem.getInfoCategory().getSubCategoryKo());
            }
        }

        // 2. 지역 정보(시/도, 시/군/구) 기반 태그 추출
        if (StringUtils.hasText(infoItem.getSido())) {
            tagNames.add(infoItem.getSido());
        }
        if (StringUtils.hasText(infoItem.getSigungu())) {
            tagNames.add(infoItem.getSigungu());
        }

        // 3. 시설/정보 명칭 및 소개글 키워드 기반 규칙 태그 추출
        String textToAnalyze = (
                (infoItem.getName() != null ? infoItem.getName() : "") + " " +
                        (infoItem.getIntroduction() != null ? infoItem.getIntroduction() : "")
        ).toLowerCase();

        if (textToAnalyze.contains("장애인") || textToAnalyze.contains("휠체어") || textToAnalyze.contains("발달장애")) {
            tagNames.add("장애인복지");
        }
        if (textToAnalyze.contains("일자리") || textToAnalyze.contains("취업") || textToAnalyze.contains("고용") || textToAnalyze.contains("자립")) {
            tagNames.add("취업지원");
        }
        if (textToAnalyze.contains("교육") || textToAnalyze.contains("학교") || textToAnalyze.contains("센터") || textToAnalyze.contains("배움")) {
            tagNames.add("교육지원");
        }
        if (textToAnalyze.contains("의료") || textToAnalyze.contains("병원") || textToAnalyze.contains("재활") || textToAnalyze.contains("치료")) {
            tagNames.add("의료재활");
        }
        if (textToAnalyze.contains("돌봄") || textToAnalyze.contains("시니어") || textToAnalyze.contains("노인")) {
            tagNames.add("돌봄서비스");
        }

        // 4. 추출된 태그 및 매핑 엔티티 저장
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName) || tagName.length() > 50) continue;

            // InfoTag 저장 또는 기존 태그 조회
            InfoTag tag = infoTagRepository.findByName(tagName)
                    .orElseGet(() -> infoTagRepository.save(
                            InfoTag.builder()
                                    .name(tagName)
                                    .build()
                    ));

            // InfoItemTag 매핑 존재 여부 체크 후 저장 (유니크 제약조건 방어)
            if (!infoItemTagRepository.existsByInfoItemAndInfoTag(infoItem, tag)) {
                infoItemTagRepository.save(
                        InfoItemTag.builder()
                                .infoItem(infoItem)
                                .infoTag(tag)
                                .build()
                );
            }
        }
    }
}