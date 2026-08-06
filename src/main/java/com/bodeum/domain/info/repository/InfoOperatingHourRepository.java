package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfoOperatingHourRepository extends JpaRepository<InfoOperatingHour, Long> {
    List<InfoOperatingHour> findAllByInfoItem(InfoItem infoItem);
    void deleteAllByInfoItem(InfoItem infoItem);
}