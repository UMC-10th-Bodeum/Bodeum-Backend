package com.bodeum.domain.region.repository;

import com.bodeum.domain.region.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByRegionLevel1AscRegionLevel2Asc();

    @Query("select region.id from Region region "
            + "where region.regionLevel1 = :regionLevel1 order by region.regionLevel2 asc")
    List<Long> findIdsByRegionLevel1(@Param("regionLevel1") String regionLevel1);
}
