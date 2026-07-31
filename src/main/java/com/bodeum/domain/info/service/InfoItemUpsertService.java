package com.bodeum.domain.info.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoOperatingHour;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.repository.InfoOperatingHourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfoItemUpsertService {

    private final InfoItemRepository infoItemRepository;
    private final InfoOperatingHourRepository infoOperatingHourRepository; // ★ 추가

    @Transactional
    public InfoItem upsert(InfoItem newItem, List<InfoOperatingHour> operatingHours) {
        Optional<InfoItem> existingItemOpt = infoItemRepository.findFirstByExternalId(newItem.getExternalId());
        InfoItem savedItem;

        if (existingItemOpt.isPresent()) {
            savedItem = existingItemOpt.get();

            savedItem.updateInformation(
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

            log.debug("기존 InfoItem 동기화 완료 - ID: {}", savedItem.getExternalId());
        } else {
            savedItem = infoItemRepository.save(newItem);
            log.debug("신규 InfoItem 저장 완료 - ID: {}", savedItem.getExternalId());
        }

        // ★ 운영시간 정보 업데이트 (기존 운영시간 삭제 후 새로 등록)
        if (operatingHours != null && !operatingHours.isEmpty()) {
            infoOperatingHourRepository.deleteAllByInfoItem(savedItem);
            for (InfoOperatingHour hour : operatingHours) {
                // InfoItem 연관관계 설정 후 저장
                hour.assignInfoItem(savedItem);
                infoOperatingHourRepository.save(hour);
            }
        }

        return savedItem;
    }
}