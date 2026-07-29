package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

// 기존 JPA 기능 + QueryDSL 동적 검색 기능 사용 가능

@Repository
public interface InfoItemRepository extends JpaRepository<InfoItem, Long>, InfoItemRepositoryCustom {

    // 고유 식별자(externalId)로 기존 데이터를 조회
    Optional<InfoItem> findByExternalId(String externalId);

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info")
    List<InfoItem> findAllIndexable();

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id in :ids")
    List<InfoItem> findAllIndexableByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id = :id")
    Optional<InfoItem> findIndexableById(@Param("id") Long id);

    // 동시성 처리를 위해 PESSIMISTIC_WRITE 락 적용 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InfoItem i WHERE i.id = :id")
    Optional<InfoItem> findByIdWithPessimisticLock(@Param("id") Long id);
}
