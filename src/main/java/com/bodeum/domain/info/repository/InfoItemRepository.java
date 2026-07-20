package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfoItemRepository extends JpaRepository<InfoItem, Long> {

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info")
    List<InfoItem> findAllIndexable();

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id in :ids")
    List<InfoItem> findAllIndexableByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "infoCategory")
    @Query("select info from InfoItem info where info.id = :id")
    Optional<InfoItem> findIndexableById(@Param("id") Long id);
}
