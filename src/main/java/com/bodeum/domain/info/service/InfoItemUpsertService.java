package com.bodeum.domain.info.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfoItemUpsertService {

    private final InfoItemRepository infoItemRepository;

    // externalId를 기준으로 데이터가 존재하면 변경 감지(Dirty Checking)를 통해 업데이트
    @Transactional
    public void upsert(InfoItem newItem) {
        Optional<InfoItem> existingItemOpt = infoItemRepository.findByExternalId(newItem.getExternalId());

        if (existingItemOpt.isPresent()) {
            InfoItem existingItem = existingItemOpt.get();

            // 엔티티 내부 수정 비즈니스 메서드 호출 (imageUrl 파라미터 추가)
            existingItem.updateInformation(
                    newItem.getName(),
                    newItem.getInfoCategory(),
                    newItem.getIntroduction(),
                    newItem.getAddress(),
                    newItem.getSido(),
                    newItem.getSigungu(),
                    newItem.getPhone(),
                    newItem.getHomepageUrl(),
                    newItem.getImageUrl() // <- 대표 이미지 URL 추가!
            );
            log.debug("기존 InfoItem 동기화 완료 - ID: {}", existingItem.getExternalId());
        } else {
            infoItemRepository.save(newItem);
            log.debug("신규 InfoItem 저장 완료 - ID: {}", newItem.getExternalId());
        }
    }
}