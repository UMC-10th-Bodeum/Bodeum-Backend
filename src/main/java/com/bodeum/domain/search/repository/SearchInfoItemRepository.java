package com.bodeum.domain.search.repository;

import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchInfoItemRepository extends JpaRepository<InfoItem, Long> {

    List<InfoItem> findByNameContaining(String keyword);
}
