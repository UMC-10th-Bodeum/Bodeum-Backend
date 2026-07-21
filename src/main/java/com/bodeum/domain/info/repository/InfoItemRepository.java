package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {
}