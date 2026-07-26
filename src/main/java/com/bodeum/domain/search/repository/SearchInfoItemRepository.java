package com.bodeum.domain.search.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchInfoItemRepository extends JpaRepository<InfoItem, Long> {

    List<InfoItem> findByNameContaining(String keyword, Pageable pageable);

    // N+1 방지를 위해 Fetch Join을 적용한 자동완성 쿼리
    @Query("SELECT DISTINCT i FROM InfoItem i " +
            "JOIN FETCH i.infoCategory " +
            "WHERE i.name LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY i.name ASC")
    List<InfoItem> findAutocompleteByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
