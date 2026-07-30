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

    @Transactional
    public void upsert(InfoItem newItem) {
        Optional<InfoItem> existingItemOpt = infoItemRepository.findByExternalId(newItem.getExternalId());

        if (existingItemOpt.isPresent()) {
            InfoItem existingItem = existingItemOpt.get();

            existingItem.updateInformation(
                    newItem.getName(),
                    newItem.getInfoCategory(),
                    newItem.getRegionId(),
                    newItem.getIntroduction(),
                    newItem.getAddress(),
                    newItem.getSido(),
                    newItem.getSigungu(),
                    newItem.getPhone(),
                    newItem.getHomepageUrl(),
                    newItem.getImageUrl()
            );

            log.debug("기존 InfoItem 동기화 완료 - ID: {}", existingItem.getExternalId());
        } else {
            infoItemRepository.save(newItem);
            log.debug("신규 InfoItem 저장 완료 - ID: {}", newItem.getExternalId());
        }
    }
}