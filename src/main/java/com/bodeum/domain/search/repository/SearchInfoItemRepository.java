package com.bodeum.domain.search.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchInfoItemRepository extends JpaRepository<InfoItem, Long> {

    List<InfoItem> findByNameContaining(String keyword, Pageable pageable);

    /**
     * 자동완성 조회 쿼리 (가중치 정렬 적용)
     * - 카테고리 키워드 매칭 (+4점)
     * - 유저 시/도(sido) 매칭 (+2점)
     * - 유저 시/군/구(sigungu) 매칭 (+1점)
     * -> 가중치 합산 내림차순 후 이름 오름차순
     */
    @Query("SELECT DISTINCT i FROM InfoItem i " +
            "JOIN FETCH i.infoCategory c " +
            "WHERE i.name LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY (" +
            "  (CASE WHEN (" +
            "    (:keyword LIKE '%기관%' OR :keyword LIKE '%복지관%' OR :keyword LIKE '%발달센터%' OR :keyword LIKE '%상담센터%' OR :keyword LIKE '%복지%' OR :keyword LIKE '%지원%' OR :keyword LIKE '%상담%' OR :keyword LIKE '%연구%') AND c.mainCategory = 'INSTITUTION'" +
            "  ) OR (" +
            "    (:keyword LIKE '%병원%' OR :keyword LIKE '%의원%' OR :keyword LIKE '%치과%' OR :keyword LIKE '%소아%' OR :keyword LIKE '%내과%' OR :keyword LIKE '%가정%' OR :keyword LIKE '%대학%') AND c.mainCategory = 'HOSPITAL'" +
            "  ) OR (" +
            "    (:keyword LIKE '%지원%' OR :keyword LIKE '%사업%' OR :keyword LIKE '%재단%' OR :keyword LIKE '%저소득%' OR :keyword LIKE '%취약%' OR :keyword LIKE '%아동%' OR :keyword LIKE '%청소년%') AND c.mainCategory = 'WELFARE'" +
            "  ) OR (" +
            "    (:keyword LIKE '%학교%' OR :keyword LIKE '%교육센터%' OR :keyword LIKE '%교육%' OR :keyword LIKE '%특수%' OR :keyword LIKE '%특수교육%' OR :keyword LIKE '%평생%') AND c.mainCategory = 'EDUCATION'" +
            "  ) THEN 4 ELSE 0 END) + " +
            "  (CASE WHEN (:sido IS NOT NULL AND i.sido = :sido) THEN 2 ELSE 0 END) + " +
            "  (CASE WHEN (:sigungu IS NOT NULL AND i.sigungu = :sigungu) THEN 1 ELSE 0 END)" +
            ") DESC, i.name ASC")
    List<InfoItem> findAutocompleteByKeyword(
            @Param("keyword") String keyword,
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            Pageable pageable
    );
}