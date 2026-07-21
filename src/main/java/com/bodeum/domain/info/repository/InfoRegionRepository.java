package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InfoRegionRepository extends JpaRepository<InfoRegion, Long> {

    // 1. 전체 시/도 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT r.sido FROM InfoRegion r ORDER BY r.sido ASC")
    List<String> findDistinctSido();

    // 2. 선택한 시/도에 속한 시/군/구 목록 조회
    @Query("SELECT r.sigungu FROM InfoRegion r WHERE r.sido = :sido ORDER BY r.sigungu ASC")
    List<String> findSigunguBySido(@Param("sido") String sido);
}