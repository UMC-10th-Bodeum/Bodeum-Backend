package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {

    // 고유 식별자(externalId)로 기존 데이터를 조회.
    Optional<InfoItem> findByExternalId(String externalId);
}