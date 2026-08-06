package com.bodeum.domain.home.repository;

import com.bodeum.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeRegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByIdIn(List<Long> ids);
}
