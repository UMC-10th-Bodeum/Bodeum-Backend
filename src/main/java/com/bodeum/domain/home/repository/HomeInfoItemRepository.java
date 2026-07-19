package com.bodeum.domain.home.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HomeInfoItemRepository extends JpaRepository<InfoItem, Long> {

    @Query("SELECT i.infoCategory.parentCategory, COUNT(i) FROM InfoItem i GROUP BY i.infoCategory.parentCategory")
    List<Object[]> countByCategory();
}
