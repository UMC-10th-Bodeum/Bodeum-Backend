package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.user.enums.InterestCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface InfoItemRepository extends JpaRepository<InfoItem, Long>, InfoItemRepositoryCustom {

    // 고유 식별자(externalId)로 기존 데이터를 조회
    Optional<InfoItem> findFirstByExternalId(String externalId);

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info")
    List<InfoItem> findAllIndexable();

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id in :ids")
    List<InfoItem> findAllIndexableByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id = :id")
    Optional<InfoItem> findIndexableById(@Param("id") Long id);

    // 동시성 처리를 위해 PESSIMISTIC_WRITE 락 적용 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InfoItem i WHERE i.id = :id")
    Optional<InfoItem> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("select i from InfoItem i join i.infoCategory c " +
            "where i.sido = :regionLevel1 and i.sigungu = :regionLevel2 " +
            "and c.subCategory = 'THERAPY_REHAB'")
    List<InfoItem> findRehabCentersByRegion(@Param("regionLevel1") String regionLevel1,
                                            @Param("regionLevel2") String regionLevel2,
                                            Pageable pageable);

    // 온보딩 유저의 지역(sido, sigungu)과 관심사 목록(interests) 조건에 맞는 아이템 조회
    List<InfoItem> findBySidoAndSigunguAndInterestIn(
            String sido,
            String sigungu,
            Collection<InterestCategory> interests
    );
}
