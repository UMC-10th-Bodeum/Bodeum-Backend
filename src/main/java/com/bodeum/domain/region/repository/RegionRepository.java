package com.bodeum.domain.region.repository;

import com.bodeum.domain.region.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByRegionLevel1AscRegionLevel2Asc();
}
