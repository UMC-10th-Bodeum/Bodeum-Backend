package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfoCategoryRepository extends JpaRepository<InfoCategory, Long> {
}