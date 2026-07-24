package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.enums.MainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfoCategoryRepository extends JpaRepository<InfoCategory, Long> {
    Optional<InfoCategory> findFirstByMainCategory(MainCategory mainCategory);
}