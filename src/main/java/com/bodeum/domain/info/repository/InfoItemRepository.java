package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
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
            "and c.subCategory = :subCategory")
    List<InfoItem> findRehabCentersByRegion(@Param("regionLevel1") String regionLevel1,
                                            @Param("regionLevel2") String regionLevel2,
                                            @Param("subCategory") InfoSubCategory subCategory,
                                            Pageable pageable);

    /**
     * 온보딩 유저 맞춤 추천 데이터 조회
     * - 지역(sido, sigungu) 및 관심사(interest IN) 필터링
     * - 가중치 점수 내림차순 정렬: (스크랩수 * 5 + 리뷰수 * 3 + 조회수 * 1)
     * - 점수가 동일할 경우 최신 생성 ID 내림차순
     */
    @Query("SELECT i FROM InfoItem i " +
            "WHERE i.sido = :sido " +
            "  AND i.sigungu = :sigungu " +
            "  AND i.interest IN :interests " +
            "  AND i.infoCategory.mainCategory = :mainCategory " +
            "ORDER BY (i.scrapCount * 5 + i.reviewCount * 3 + i.viewCount) DESC, i.id DESC")
    List<InfoItem> findBySidoAndSigunguAndInterestInAndMainCategory(
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("interests") Collection<InterestCategory> interests,
            @Param("mainCategory") MainCategory mainCategory
    );
}
