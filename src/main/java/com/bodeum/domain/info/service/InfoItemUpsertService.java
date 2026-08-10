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
    public InfoItem upsert(InfoItem newItem) {
        Optional<InfoItem> existingItemOpt = infoItemRepository.findFirstByExternalId(newItem.getExternalId());
        InfoItem savedItem;

        if (existingItemOpt.isPresent()) {
            savedItem = existingItemOpt.get();

            savedItem.updateInformation(
                    newItem.getName(),
                    newItem.getInfoCategory(),
                    newItem.getInterest(),
                    newItem.getRegionId(),
                    newItem.getIntroduction(),
                    newItem.getAddress(),
                    newItem.getSido(),
                    newItem.getSigungu(),
                    newItem.getPhone(),
                    newItem.getHomepageUrl(),
                    newItem.getImageUrl()
            );

            log.debug("기존 InfoItem 동기화 완료 - ID: {}", savedItem.getExternalId());
        } else {
            savedItem = infoItemRepository.save(newItem);
            log.debug("신규 InfoItem 저장 완료 - ID: {}", savedItem.getExternalId());
        }

        return savedItem;
    }
}